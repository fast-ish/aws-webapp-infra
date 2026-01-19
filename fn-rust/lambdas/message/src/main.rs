use aws_lambda_events::event::cognito::CognitoEventUserPoolsCustomMessage;
use lambda_common::{Error, Result};
use lambda_observability::{init_logging, logging::invocation_span};
use lambda_runtime::{run, service_fn, Error as LambdaError, LambdaEvent};
use serde::{Deserialize, Serialize};
use tracing::info;

/// Cognito trigger sources for custom messages
#[derive(Debug, PartialEq)]
enum TriggerSource {
    CustomMessageSignUp,
    CustomMessageResendCode,
    CustomMessageForgotPassword,
    CustomMessageUpdateUserAttribute,
    CustomMessageVerifyUserAttribute,
    Unknown,
}

impl From<&str> for TriggerSource {
    fn from(s: &str) -> Self {
        match s {
            "CustomMessage_SignUp" => TriggerSource::CustomMessageSignUp,
            "CustomMessage_ResendCode" => TriggerSource::CustomMessageResendCode,
            "CustomMessage_ForgotPassword" => TriggerSource::CustomMessageForgotPassword,
            "CustomMessage_UpdateUserAttribute" => TriggerSource::CustomMessageUpdateUserAttribute,
            "CustomMessage_VerifyUserAttribute" => TriggerSource::CustomMessageVerifyUserAttribute,
            _ => TriggerSource::Unknown,
        }
    }
}

/// Custom response type that matches Java implementation
#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct CustomMessageResponse {
    sms_message: Option<String>,
    email_subject: Option<String>,
    email_message: Option<String>,
}

async fn function_handler(
    event: LambdaEvent<CognitoEventUserPoolsCustomMessage>,
) -> Result<CognitoEventUserPoolsCustomMessage> {
    let (mut event, context) = event.into_parts();

    let _span = invocation_span("message", &context.request_id);

    info!("Processing Cognito custom message event");

    let trigger_source = event
        .cognito_event_user_pools_header
        .trigger_source
        .as_ref()
        .map(|ts| format!("{:?}", ts))
        .unwrap_or_default();
    let trigger_source = TriggerSource::from(trigger_source.as_str());

    // Get username for personalization
    let username = event
        .request
        .user_attributes
        .get("preferred_username")
        .or_else(|| event.request.user_attributes.get("email"))
        .and_then(|v| v.as_str())
        .map(|s| s.to_string())
        .unwrap_or_else(|| "User".to_string());

    // Get code placeholder - Cognito replaces {####} with actual code
    let code_placeholder = "{####}";

    match trigger_source {
        TriggerSource::CustomMessageSignUp => {
            info!("Custom message signup");
            event.response.sms_message = Some(format!(
                "Please use verification code '{}' to validate your phone number.",
                code_placeholder
            ));
            event.response.email_subject = Some("webapp verification".to_string());
            event.response.email_message = Some(format!(
                r#"<body style="user-select: none; background-color: #f9f9f9; max-width: 64rem; margin-left: auto; margin-right: auto; font-family: 'Source Sans Pro',monospace;">
<h1 style="color: #ffcce0;">webapp</h1>
<div>
    <p>Hey {},</p><br><br>
</div>
<div>
    <p>Welcome to webapp!</p>
</div>
<div>
    <p>
        Please use this confirmation code to finish your onboarding, <code style="user-select: text; font-weight: 800; font-size: 1.5rem; line-height: 2rem;">{}</code>.
    </p>
</div>
Thanks,<br>
:)<br>
</body>"#,
                username, code_placeholder
            ));
        }
        TriggerSource::CustomMessageResendCode => {
            info!("Custom message resend code");
            event.response.sms_message = Some(format!(
                "Your new verification code is '{}'.",
                code_placeholder
            ));
            event.response.email_subject = Some("webapp - Resend Code".to_string());
            event.response.email_message = Some(format!(
                r#"<body style="user-select: none; background-color: #f9f9f9; max-width: 64rem; margin-left: auto; margin-right: auto; font-family: 'Source Sans Pro',monospace;">
<h1 style="color: #ffcce0;">webapp</h1>
<div>
    <p>Hey {},</p><br><br>
</div>
<div>
    <p>Your new verification code is: <code style="user-select: text; font-weight: 800; font-size: 1.5rem; line-height: 2rem;">{}</code>.</p>
</div>
Thanks,<br>
:)<br>
</body>"#,
                username, code_placeholder
            ));
        }
        TriggerSource::CustomMessageForgotPassword => {
            info!("Custom message forgot password");
            event.response.sms_message = Some(format!(
                "Your password reset code is '{}'.",
                code_placeholder
            ));
            event.response.email_subject = Some("webapp - Password Reset".to_string());
            event.response.email_message = Some(format!(
                r#"<body style="user-select: none; background-color: #f9f9f9; max-width: 64rem; margin-left: auto; margin-right: auto; font-family: 'Source Sans Pro',monospace;">
<h1 style="color: #ffcce0;">webapp</h1>
<div>
    <p>Hey {},</p><br><br>
</div>
<div>
    <p>Your password reset code is: <code style="user-select: text; font-weight: 800; font-size: 1.5rem; line-height: 2rem;">{}</code>.</p>
</div>
Thanks,<br>
:)<br>
</body>"#,
                username, code_placeholder
            ));
        }
        TriggerSource::CustomMessageUpdateUserAttribute => {
            info!("Custom message update attribute");
            event.response.sms_message = Some(format!(
                "Your attribute update verification code is '{}'.",
                code_placeholder
            ));
            event.response.email_subject = Some("webapp - Update Attribute".to_string());
            event.response.email_message = Some(format!(
                r#"<body style="user-select: none; background-color: #f9f9f9; max-width: 64rem; margin-left: auto; margin-right: auto; font-family: 'Source Sans Pro',monospace;">
<h1 style="color: #ffcce0;">webapp</h1>
<div>
    <p>Hey {},</p><br><br>
</div>
<div>
    <p>Your attribute update verification code is: <code style="user-select: text; font-weight: 800; font-size: 1.5rem; line-height: 2rem;">{}</code>.</p>
</div>
Thanks,<br>
:)<br>
</body>"#,
                username, code_placeholder
            ));
        }
        TriggerSource::CustomMessageVerifyUserAttribute => {
            info!("Custom message verify attribute");
            event.response.sms_message = Some(format!(
                "Your attribute verification code is '{}'.",
                code_placeholder
            ));
            event.response.email_subject = Some("webapp - Verify Attribute".to_string());
            event.response.email_message = Some(format!(
                r#"<body style="user-select: none; background-color: #f9f9f9; max-width: 64rem; margin-left: auto; margin-right: auto; font-family: 'Source Sans Pro',monospace;">
<h1 style="color: #ffcce0;">webapp</h1>
<div>
    <p>Hey {},</p><br><br>
</div>
<div>
    <p>Your attribute verification code is: <code style="user-select: text; font-weight: 800; font-size: 1.5rem; line-height: 2rem;">{}</code>.</p>
</div>
Thanks,<br>
:)<br>
</body>"#,
                username, code_placeholder
            ));
        }
        TriggerSource::Unknown => {
            info!("Unknown trigger source, returning event unchanged");
        }
    }

    Ok(event)
}

#[tokio::main]
async fn main() -> std::result::Result<(), lambda_runtime::Error> {
    init_logging();
    info!("Message customization Lambda function initialized");
    run(service_fn(function_handler)).await
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_trigger_source_from_string() {
        assert_eq!(
            TriggerSource::from("CustomMessage_SignUp"),
            TriggerSource::CustomMessageSignUp
        );
        assert_eq!(
            TriggerSource::from("CustomMessage_ResendCode"),
            TriggerSource::CustomMessageResendCode
        );
        assert_eq!(TriggerSource::from("Unknown"), TriggerSource::Unknown);
    }
}
