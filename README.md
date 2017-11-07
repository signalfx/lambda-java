# SignalFx Java Lambda Wrapper

SignalFx Java Lambda Wrapper.

## Supported Languages

* Java 7+

## Usage

The SignalFx Java Lambda Wrapper is a wrapper around an AWS Lambda Java function handler, used to instrument execution of the function and send metrics to SignalFx.

### Download jar file
Use the jar file from https://cdn.signalfx.com/signalfx-lambda-0.0.4.jar and install it to your local maven repository with following parameters:
- groupId: `com.signalfx.public`
- artifactId: `signalfx-lambda`
- version: `0.0.4`

### Install via maven dependency
```xml
<dependency>
  <groupId>com.signalfx.public</groupId>
  <artifactId>signalfx-lambda</artifactId>
  <version>0.0.4</version>
</dependency>
```

###  Package
Package jar file and upload to AWS per instructions [here](http://docs.aws.amazon.com/lambda/latest/dg/java-create-jar-pkg-maven-no-ide.html).

### Using SignalFx Handler (recommended)
Configure Handler for the function in AWS to be:

* `com.signalfx.lambda.wrapper.SignalFxRequestWrapper::handleRequest` for normal Input/Output request
* `com.signalfx.lambda.wrapper.SignalFxRequestStreamWrapper::handleRequest` for normal Stream request

### Using your own Handler
Manually wrap the code inside the handler as followed:
```java
// in your handler
MetricWrapper wrapper = new MetricWrapper(context)
try {
    // your code
} catch (Exception e) {
    wrapper.error();
} finally {
    wrapper.close();
}
```

### Environment Variable
Set the Lambda environment variables as follows:

1) Set authentication token:
```
 SIGNALFX_AUTH_TOKEN=signalfx token
```
2) Set the handler function in format `package.ClassName::methodName` (skip this if you use your own handler):
```
SIGNALFX_LAMBDA_HANDLER=com.signalfx.lambda.example.CustomHandler::handler
```
3) Optional parameters available:
```
 SIGNALFX_API_HOSTNAME=[ingest.signalfx.com]
 SIGNALFX_API_PORT=[443]
 SIGNALFX_API_SCHEME=[https]
 SIGNALFX_SEND_TIMEOUT=milliseconds for signalfx client timeout [2000]
```

### Metrics and dimensions sent by the wrapper

The Lambda wrapper sends the following metrics to SignalFx:

| Metric Name  | Type | Description |
| ------------- | ------------- | ---|
| function.invocations  | Counter  | Count number of Lambda invocations|
| function.cold_starts  | Counter  | Count number of cold starts|
| function.errors  | Counter  | Count number of errors from underlying Lambda handler|
| function.duration  | Gauge  | Milliseconds in execution time of underlying Lambda handler|

The Lambda wrapper adds the following dimensions to all data points sent to SignalFx:

| Dimension | Description |
| ------------- | ---|
| lambda_arn  | ARN of the Lambda function instance |
| aws_region  | AWS Region  |
| aws_account_id | AWS Account ID  |
| aws_function_name  | AWS Function Name |
| aws_function_version  | AWS Function Version |
| aws_function_qualifier  | AWS Function Version Qualifier (version or version alias if it is not an event source mapping Lambda invocation) |
| event_source_mappings  | AWS Function Name (if it is an event source mapping Lambda invocation) |
| aws_execution_env  | AWS execution environment (e.g. AWS_Lambda_java8) |
| function_wrapper_version  | SignalFx function wrapper qualifier (e.g. signalfx-lambda-0.0.4) |
| metric_source | the literal value of 'lambda_wrapper' |

### Sending a metric from the Lambda function
```java
// construct data point builder
SignalFxProtocolBuffers.DataPoint.Builder builder =
        SignalFxProtocolBuffers.DataPoint.newBuilder()
                .setMetric("application.metric")
                .setMetricType(SignalFxProtocolBuffers.MetricType.GAUGE)
                .setValue(
                        SignalFxProtocolBuffers.Datum.newBuilder()
                                .setDoubleValue(100));

// add custom dimension
builder.addDimensionsBuilder().setKey("applicationName").setValue("CoolApp").build();

// send the metric
MetricSender.sendMetric(builder);
```

## Testing
Test example is available at `com.signalfx.lambda.example.CustomHandler::handler`. Make appropriate changes if needed.

### Testing locally.
1) Set test input event and lambda function handler:
```
LAMBDA_INPUT_EVENT={"abc": "def"}
SIGNALFX_LAMBDA_HANDLER=com.signalfx.lambda.example.CustomHandler::handler
```
2) Run `mvn compile exec:java`.

### Testing from the AWS Console
1) Run `mvn clean compile package -Ptest` to package using the test profile, which will include the runner and test handler.

2) Set the SignalFx Lambda handler environment variable to either
`com.signalfx.lambda.example.CustomHandler::handler` or `com.signalfx.lambda.example.CustomStreamHandler::handleRequest`.

## License

Apache Software License v2. Copyright © 2014-2017 SignalFx
