# SignalFx Java Lambda Wrapper

SignalFx Java Lambda Wrapper.

## Testing
Test example is available at `com.signalfx.lambda.example.CustomHandler::handler`. Make appropriate changes if needed.

### Environment Variable
Either locally or on aws, following environment variable needs to be set:

1) Set auth token variables:
```
 SIGNALFX_AUTH_TOKEN=signalfx token
```
2) Set the handler function in format `package.ClassName::methodName`
```
SIGNALFX_LAMBDA_HANDLER=com.signalfx.lambda.TestCustomHandler::handler
```
3) Optional parameters available:
```
 SIGNALFX_API_HOSTNAME=[ingest.signalfx.com]
 SIGNALFX_API_PORT=[443]
 SIGNALFX_API_SCHEME=[https]
 SIGNALFX_SEND_TIMEOUT=milli second for signalfx client timeout [2000]
```

### Testing locally.
1) Set test input event and lambda function handler
```
LAMBDA_INPUT_EVENT={"abc":"def"}
SIGNALFX_LAMBDA_HANDLER=com.signalfx.lambda.TestCustomHandler::handler
```
2) run `mvn compile exec:java`

### Testing from the AWS Console
1) Run `mvn clean compile package -Ptest` to package using test profile that will include runner and test handler.
2) In the AWS Console, author a Lambda function from scratch.
3) Fill in required fields. Change "Code entry type" to "Upload a .ZIP file"
and upload target/<mvn-package-name>-1.0-SNAPSHOT.jar.
4) Set handler to
- `com.signalfx.lambda.wrapper.SignalFxRequestWrapper::handleRequest` for normal Input/Output request
- `com.signalfx.lambda.wrapper.SignalFxRequestStreamWrapper::handleRequest` for normal Stream request