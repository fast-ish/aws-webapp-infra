use aws_sdk_cognitoidentityprovider::Client as CognitoClient;
use aws_sdk_dynamodb::types::AttributeValue;
use aws_sdk_dynamodb::Client as DynamoDbClient;
use lambda_aws_clients::{cognito, dynamodb};
use lambda_common::{Error, Result};
use lambda_http::{run, service_fn, Body, Request, RequestExt, Response};
use lambda_observability::{init_logging, logging::invocation_span};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::env;
use tracing::{error, info};

/// User model matching Java implementation
#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct User {
    id: String,
    email: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    phone: Option<String>,
    username: String,
    settings: Settings,
    verification: Verification,
    updated: String,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct Settings {
    mfa: Mfa,
    theme: String,
    subscription: String,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct Mfa {
    enabled: bool,
    method: String,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct Verification {
    email: bool,
    phone: bool,
    terms: bool,
    status: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct UpdateUserRequest {
    phone: String,
    username: String,
    settings: Settings,
}

async fn function_handler(event: Request) -> Result<Response<Body>> {
    let request_id = "user-request";
    let _span = invocation_span("user", request_id);

    info!("Processing user API request: {:?}", event);

    let method = event.method().as_str();
    let path = event.uri().path();
    let path_params = event.path_parameters();

    let user_id = path_params
        .first("user")
        .ok_or_else(|| Error::InvalidInput("Missing user parameter".to_string()))?;

    info!("Request: {} {} for user: {}", method, path, user_id);

    let dynamo_client = dynamodb::client().await;
    let cognito_client = cognito::client().await;

    match (method, path.contains("/unsubscribe")) {
        ("GET", false) => handle_get_user(&dynamo_client, user_id).await,
        ("PUT", false) => {
            let body = event.body();
            let body_str = std::str::from_utf8(body).map_err(|e| {
                Error::InvalidInput(format!("Invalid UTF-8 in request body: {}", e))
            })?;
            let update_request: UpdateUserRequest = serde_json::from_str(body_str)
                .map_err(|e| Error::InvalidInput(format!("Invalid JSON: {}", e)))?;
            handle_update_user(&dynamo_client, user_id, update_request).await
        }
        ("DELETE", true) => handle_delete_user(&dynamo_client, &cognito_client, user_id).await,
        _ => {
            error!("Invalid request: {} {}", method, path);
            Ok(Response::builder()
                .status(400)
                .header("Content-Type", "application/json")
                .body(Body::from(r#"{"error":"Invalid request method or path"}"#))
                .unwrap())
        }
    }
}

async fn handle_get_user(client: &DynamoDbClient, user_id: &str) -> Result<Response<Body>> {
    let table_name = env::var("DYNAMODB_USER_TABLE")
        .map_err(|_| Error::Configuration("DYNAMODB_USER_TABLE not set".to_string()))?;

    info!("Getting user: {}", user_id);

    let result = client
        .get_item()
        .table_name(table_name)
        .key("id", AttributeValue::S(user_id.to_string()))
        .consistent_read(true)
        .send()
        .await
        .map_err(|e| Error::AwsSdk(format!("Failed to get user: {:?}", e)))?;

    if let Some(item) = result.item {
        let user = parse_user_from_dynamodb(item)?;
        let body = serde_json::to_string(&user)?;

        info!("Successfully retrieved user: {}", user_id);

        Ok(Response::builder()
            .status(200)
            .header("Content-Type", "application/json")
            .body(Body::from(body))
            .unwrap())
    } else {
        error!("User not found: {}", user_id);
        Ok(Response::builder()
            .status(404)
            .header("Content-Type", "application/json")
            .body(Body::from(r#"{"error":"User not found"}"#))
            .unwrap())
    }
}

async fn handle_update_user(
    client: &DynamoDbClient,
    user_id: &str,
    request: UpdateUserRequest,
) -> Result<Response<Body>> {
    let table_name = env::var("DYNAMODB_USER_TABLE")
        .map_err(|_| Error::Configuration("DYNAMODB_USER_TABLE not set".to_string()))?;

    info!("Updating user: {}", user_id);

    let updated = chrono::Utc::now().to_rfc3339();

    let settings_map = settings_to_attribute_value(&request.settings);

    let result = client
        .update_item()
        .table_name(table_name)
        .key("id", AttributeValue::S(user_id.to_string()))
        .update_expression(
            "SET phone = :phone, username = :username, settings = :settings, updated = :updated",
        )
        .expression_attribute_values(":phone", AttributeValue::S(request.phone.clone()))
        .expression_attribute_values(":username", AttributeValue::S(request.username.clone()))
        .expression_attribute_values(":settings", settings_map)
        .expression_attribute_values(":updated", AttributeValue::S(updated))
        .return_values(aws_sdk_dynamodb::types::ReturnValue::AllNew)
        .send()
        .await
        .map_err(|e| Error::AwsSdk(format!("Failed to update user: {:?}", e)))?;

    if let Some(attributes) = result.attributes {
        let user = parse_user_from_dynamodb(attributes)?;
        let body = serde_json::to_string(&user)?;

        info!("Successfully updated user: {}", user_id);

        Ok(Response::builder()
            .status(200)
            .header("Content-Type", "application/json")
            .body(Body::from(body))
            .unwrap())
    } else {
        error!("Failed to update user: {}", user_id);
        Ok(Response::builder()
            .status(400)
            .header("Content-Type", "application/json")
            .body(Body::from(r#"{"error":"Failed to update user"}"#))
            .unwrap())
    }
}

async fn handle_delete_user(
    dynamo_client: &DynamoDbClient,
    cognito_client: &CognitoClient,
    user_id: &str,
) -> Result<Response<Body>> {
    let table_name = env::var("DYNAMODB_USER_TABLE")
        .map_err(|_| Error::Configuration("DYNAMODB_USER_TABLE not set".to_string()))?;
    let user_pool_name = env::var("USER_POOL_NAME")
        .map_err(|_| Error::Configuration("USER_POOL_NAME not set".to_string()))?;

    info!("Deleting user: {}", user_id);

    // First, delete from Cognito
    let pools = cognito_client
        .list_user_pools()
        .max_results(60)
        .send()
        .await
        .map_err(|e| Error::AwsSdk(format!("Failed to list user pools: {:?}", e)))?;

    if let Some(user_pools) = pools.user_pools {
        if let Some(pool) = user_pools
            .iter()
            .find(|p| p.name.as_deref() == Some(&user_pool_name))
        {
            if let Some(pool_id) = &pool.id {
                cognito_client
                    .admin_delete_user()
                    .user_pool_id(pool_id)
                    .username(user_id)
                    .send()
                    .await
                    .map_err(|e| {
                        error!("Failed to delete user from Cognito: {:?}", e);
                        Error::AwsSdk(format!("Failed to delete user from Cognito: {:?}", e))
                    })?;
                info!("Deleted user from Cognito: {}", user_id);
            }
        }
    }

    // Then delete from DynamoDB
    dynamo_client
        .delete_item()
        .table_name(table_name)
        .key("id", AttributeValue::S(user_id.to_string()))
        .send()
        .await
        .map_err(|e| Error::AwsSdk(format!("Failed to delete user from DynamoDB: {:?}", e)))?;

    info!("Successfully deleted user: {}", user_id);

    Ok(Response::builder()
        .status(200)
        .header("Content-Type", "application/json")
        .body(Body::from(r#"{"success":true}"#))
        .unwrap())
}

fn parse_user_from_dynamodb(item: HashMap<String, AttributeValue>) -> Result<User> {
    let id = item
        .get("id")
        .and_then(|v| v.as_s().ok())
        .ok_or_else(|| Error::InvalidInput("Missing id".to_string()))?
        .clone();

    let email = item
        .get("email")
        .and_then(|v| v.as_s().ok())
        .ok_or_else(|| Error::InvalidInput("Missing email".to_string()))?
        .clone();

    let phone = item.get("phone").and_then(|v| v.as_s().ok()).cloned();

    let username = item
        .get("username")
        .and_then(|v| v.as_s().ok())
        .ok_or_else(|| Error::InvalidInput("Missing username".to_string()))?
        .clone();

    let settings_map = item
        .get("settings")
        .and_then(|v| v.as_m().ok())
        .ok_or_else(|| Error::InvalidInput("Missing settings".to_string()))?;

    let settings = parse_settings(settings_map)?;

    let verification_map = item
        .get("verification")
        .and_then(|v| v.as_m().ok())
        .ok_or_else(|| Error::InvalidInput("Missing verification".to_string()))?;

    let verification = parse_verification(verification_map)?;

    let updated = item
        .get("updated")
        .and_then(|v| v.as_s().ok())
        .ok_or_else(|| Error::InvalidInput("Missing updated".to_string()))?
        .clone();

    Ok(User {
        id,
        email,
        phone,
        username,
        settings,
        verification,
        updated,
    })
}

fn parse_settings(map: &HashMap<String, AttributeValue>) -> Result<Settings> {
    let mfa_map = map
        .get("mfa")
        .and_then(|v| v.as_m().ok())
        .ok_or_else(|| Error::InvalidInput("Missing mfa in settings".to_string()))?;

    let mfa = Mfa {
        enabled: mfa_map
            .get("enabled")
            .and_then(|v| v.as_bool().ok())
            .copied()
            .unwrap_or(false),
        method: mfa_map
            .get("method")
            .and_then(|v| v.as_s().ok())
            .unwrap_or(&"none".to_string())
            .clone(),
    };

    let theme = map
        .get("theme")
        .and_then(|v| v.as_s().ok())
        .unwrap_or(&"light".to_string())
        .clone();

    let subscription = map
        .get("subscription")
        .and_then(|v| v.as_s().ok())
        .unwrap_or(&"FREE".to_string())
        .clone();

    Ok(Settings {
        mfa,
        theme,
        subscription,
    })
}

fn parse_verification(map: &HashMap<String, AttributeValue>) -> Result<Verification> {
    Ok(Verification {
        email: map
            .get("email")
            .and_then(|v| v.as_bool().ok())
            .copied()
            .unwrap_or(false),
        phone: map
            .get("phone")
            .and_then(|v| v.as_bool().ok())
            .copied()
            .unwrap_or(false),
        terms: map
            .get("terms")
            .and_then(|v| v.as_bool().ok())
            .copied()
            .unwrap_or(false),
        status: map
            .get("status")
            .and_then(|v| v.as_s().ok())
            .unwrap_or(&"UNKNOWN".to_string())
            .clone(),
    })
}

fn settings_to_attribute_value(settings: &Settings) -> AttributeValue {
    let mut settings_map = HashMap::new();

    let mut mfa_map = HashMap::new();
    mfa_map.insert(
        "enabled".to_string(),
        AttributeValue::Bool(settings.mfa.enabled),
    );
    mfa_map.insert(
        "method".to_string(),
        AttributeValue::S(settings.mfa.method.clone()),
    );

    settings_map.insert("mfa".to_string(), AttributeValue::M(mfa_map));
    settings_map.insert(
        "theme".to_string(),
        AttributeValue::S(settings.theme.clone()),
    );
    settings_map.insert(
        "subscription".to_string(),
        AttributeValue::S(settings.subscription.clone()),
    );

    AttributeValue::M(settings_map)
}

#[tokio::main]
async fn main() -> std::result::Result<(), lambda_runtime::Error> {
    init_logging();
    info!("User API Lambda function initialized");
    run(service_fn(function_handler)).await
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_settings_to_attribute_value() {
        let settings = Settings {
            mfa: Mfa {
                enabled: true,
                method: "totp".to_string(),
            },
            theme: "dark".to_string(),
            subscription: "PRO".to_string(),
        };

        let av = settings_to_attribute_value(&settings);
        assert!(av.as_m().is_ok());
    }
}
