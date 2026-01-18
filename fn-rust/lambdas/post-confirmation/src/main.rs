use aws_lambda_events::event::cognito::CognitoEventUserPoolsPostConfirmation;
use aws_sdk_cognitoidentityprovider::Client as CognitoClient;
use aws_sdk_dynamodb::types::AttributeValue;
use aws_sdk_dynamodb::Client as DynamoDbClient;
use lambda_aws_clients::{cognito, dynamodb};
use lambda_common::{Error, Result};
use lambda_observability::{init_logging, logging::invocation_span};
use lambda_runtime::{run, service_fn, Error as LambdaError, LambdaEvent};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::env;
use tracing::{error, info};

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct UserSettings {
    mfa: MfaSettings,
    theme: String,
    subscription: String,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct MfaSettings {
    enabled: bool,
    method: String,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct UserVerification {
    email: bool,
    phone: bool,
    terms: bool,
    status: String,
}

async fn function_handler(
    event: LambdaEvent<CognitoEventUserPoolsPostConfirmation>,
) -> Result<CognitoEventUserPoolsPostConfirmation> {
    let (event_data, context) = event.into_parts();

    let _span = invocation_span("post-confirmation", &context.request_id);

    info!(
        "Processing post-confirmation event for user: {}",
        event_data.cognito_event_user_pools_header.user_name.as_deref().unwrap_or("unknown")
    );

    let dynamo_client = dynamodb::client().await;
    let cognito_client = cognito::client().await;

    // Initialize user - create DynamoDB record
    match remember_user(&dynamo_client, &event_data).await {
        Ok(_) => info!("Successfully created user record"),
        Err(e) => {
            error!("Error creating user record: {:?}", e);
            // Don't fail the lambda, just log the error
        }
    }

    // Add user to default group
    match group_user(&cognito_client, &event_data).await {
        Ok(_) => info!("Successfully added user to group"),
        Err(e) => {
            error!("Error adding user to group: {:?}", e);
            // Don't fail the lambda, just log the error
        }
    }

    Ok(event_data)
}

async fn remember_user(
    client: &DynamoDbClient,
    event: &CognitoEventUserPoolsPostConfirmation,
) -> Result<()> {
    let table_name = env::var("DYNAMODB_USER_TABLE")
        .map_err(|_| Error::Configuration("DYNAMODB_USER_TABLE not set".to_string()))?;

    let user_id = event.cognito_event_user_pools_header.user_name.as_deref().unwrap_or("unknown");

    info!("Checking if user exists: {}", user_id);

    // Check if user already exists
    let get_result = client
        .get_item()
        .table_name(&table_name)
        .key("id", AttributeValue::S(user_id.to_string()))
        .send()
        .await
        .map_err(|e| Error::AwsSdk(format!("Failed to check user existence: {:?}", e)))?;

    if get_result.item.is_some() {
        info!("User already exists: {}", user_id);
        return Ok(());
    }

    info!("Creating new user: {}", user_id);

    // Extract user attributes
    let email = event
        .request
        .user_attributes
        .get("email")
        .cloned()
        .unwrap_or_default();

    let phone = event.request.user_attributes.get("phone_number").cloned();

    let username = event
        .request
        .user_attributes
        .get("preferred_username")
        .cloned()
        .unwrap_or_else(|| email.clone());

    let email_verified = parse_bool(
        event
            .request
            .user_attributes
            .get("email_verified")
            .map(|s| s.as_str()),
    );

    let phone_verified = parse_bool(
        event
            .request
            .user_attributes
            .get("phone_number_verified")
            .map(|s| s.as_str()),
    );

    let terms = parse_bool(
        event
            .request
            .user_attributes
            .get("custom:terms")
            .map(|s| s.as_str()),
    );

    let status = event
        .request
        .user_attributes
        .get("cognito:user_status")
        .cloned()
        .unwrap_or_else(|| "CONFIRMED".to_string());

    // Get theme from client metadata if available
    let theme = event
        .request
        .client_metadata
        .get("theme")
        .cloned()
        .unwrap_or_else(|| "light".to_string());

    // Parse MFA settings from custom:mfa attribute
    let mfa_settings = if let Some(mfa_str) = event.request.user_attributes.get("custom:mfa") {
        serde_json::from_str::<MfaSettings>(mfa_str).unwrap_or(MfaSettings {
            enabled: false,
            method: "none".to_string(),
        })
    } else {
        MfaSettings {
            enabled: false,
            method: "none".to_string(),
        }
    };

    // Build user item
    let mut item = HashMap::new();
    item.insert("id".to_string(), AttributeValue::S(user_id.to_string()));
    item.insert("email".to_string(), AttributeValue::S(email));
    item.insert("username".to_string(), AttributeValue::S(username));

    if let Some(phone_number) = phone {
        item.insert("phone".to_string(), AttributeValue::S(phone_number));
    }

    // Build settings
    let mut settings_map = HashMap::new();
    let mut mfa_map = HashMap::new();
    mfa_map.insert(
        "enabled".to_string(),
        AttributeValue::Bool(mfa_settings.enabled),
    );
    mfa_map.insert("method".to_string(), AttributeValue::S(mfa_settings.method));
    settings_map.insert("mfa".to_string(), AttributeValue::M(mfa_map));
    settings_map.insert("theme".to_string(), AttributeValue::S(theme));
    settings_map.insert(
        "subscription".to_string(),
        AttributeValue::S("FREE".to_string()),
    );

    item.insert("settings".to_string(), AttributeValue::M(settings_map));

    // Build verification
    let mut verification_map = HashMap::new();
    verification_map.insert("email".to_string(), AttributeValue::Bool(email_verified));
    verification_map.insert("phone".to_string(), AttributeValue::Bool(phone_verified));
    verification_map.insert("terms".to_string(), AttributeValue::Bool(terms));
    verification_map.insert("status".to_string(), AttributeValue::S(status));

    item.insert(
        "verification".to_string(),
        AttributeValue::M(verification_map),
    );

    // Add timestamp
    let updated = chrono::Utc::now().to_rfc3339();
    item.insert("updated".to_string(), AttributeValue::S(updated));

    // Put item in DynamoDB
    client
        .put_item()
        .table_name(table_name)
        .set_item(Some(item))
        .send()
        .await
        .map_err(|e| Error::AwsSdk(format!("Failed to create user: {:?}", e)))?;

    info!("Successfully created user: {}", user_id);
    Ok(())
}

async fn group_user(
    client: &CognitoClient,
    event: &CognitoEventUserPoolsPostConfirmation,
) -> Result<()> {
    let user_pool_id = event.cognito_event_user_pools_header.user_pool_id.as_deref().unwrap_or("");
    let username = event.cognito_event_user_pools_header.user_name.as_deref().unwrap_or("unknown");
    let group_name = "free";

    info!("Checking if user is already in group: {}", username);

    // Check if user is already in the group
    let list_result = client
        .admin_list_groups_for_user()
        .user_pool_id(user_pool_id)
        .username(username)
        .send()
        .await
        .map_err(|e| Error::AwsSdk(format!("Failed to list user groups: {:?}", e)))?;

    let is_in_group = list_result
        .groups
        .unwrap_or_default()
        .iter()
        .any(|g| g.group_name.as_deref() == Some(group_name));

    if is_in_group {
        info!("User already in group: {}", username);
        return Ok(());
    }

    info!("Adding user to group: {}", group_name);

    // Add user to group
    client
        .admin_add_user_to_group()
        .user_pool_id(user_pool_id)
        .username(username)
        .group_name(group_name)
        .send()
        .await
        .map_err(|e| Error::AwsSdk(format!("Failed to add user to group: {:?}", e)))?;

    info!("Successfully added user to group: {}", username);
    Ok(())
}

fn parse_bool(value: Option<&str>) -> bool {
    match value {
        Some("true") | Some("True") | Some("TRUE") => true,
        Some("false") | Some("False") | Some("FALSE") => false,
        _ => false,
    }
}

#[tokio::main]
async fn main() -> std::result::Result<(), lambda_runtime::Error> {
    init_logging();
    info!("Post-confirmation Lambda function initialized");
    run(service_fn(function_handler)).await
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_bool() {
        assert_eq!(parse_bool(Some("true")), true);
        assert_eq!(parse_bool(Some("True")), true);
        assert_eq!(parse_bool(Some("TRUE")), true);
        assert_eq!(parse_bool(Some("false")), false);
        assert_eq!(parse_bool(Some("False")), false);
        assert_eq!(parse_bool(Some("FALSE")), false);
        assert_eq!(parse_bool(None), false);
        assert_eq!(parse_bool(Some("invalid")), false);
    }
}
