# SignalFx Java Lambda Wrapper

## Supported Languages

* Java 8+

## Overview

The SignalFx Java Lambda Wrapper is a wrapper around an AWS Lambda Java function handler, used to instrument execution of the function and send metrics to SignalFx.

### Step 1: Install via maven dependency
```xml
<dependency>
  <groupId>com.signalfx.public</groupId>
  <artifactId>signalfx-lambda</artifactId>
  <version>0.0.6</version>
</dependency>
```

### Step 2: Package
Package jar file and upload to AWS per instructions [here](http://docs.aws.amazon.com/lambda/latest/dg/java-create-jar-pkg-maven-no-ide.html).

### Step 3: Choose one of the following wrapping options.
### Option 1: Using SignalFx Handler (recommended)
##### Step 1: Configure Handler for the function in AWS

Configure Handler for the function in AWS to be:
* `com.signalfx.lambda.wrapper.SignalFxRequestWrapper::handleRequest` for normal Input/Output request. Review the [example](https://github.com/signalfx/lambda-java/blob/master/src/java/com/signalfx/lambda/example/CustomRequestHandler.java). 
* `com.signalfx.lambda.wrapper.SignalFxRequestStreamWrapper::handleRequest` for normal Stream request. Review the [example](https://github.com/signalfx/lambda-java/blob/master/src/java/com/signalfx/lambda/example/CustomStreamHandler.java).

For available function signatures, check https://github.com/signalfx/lambda-java/tree/master/src/java/com/signalfx/lambda/example 

##### Step 2: Set the handler function using SIGNALFX_LAMBDA_HANDLER environment variable
Set the handler function using SIGNALFX_LAMBDA_HANDLER environment variable. The format of the handler needs to be `package.ClassName::methodName`, for example `com.signalfx.lambda.example.CustomHandler::handler`.
```
SIGNALFX_LAMBDA_HANDLER=com.signalfx.lambda.example.CustomHandler::handler
```

### Option 2: Manually wrap the function

##### Step 1: Manually wrap the code inside the handler as followed:

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
Review the [example](https://github.com/signalfx/lambda-java/blob/master/src/java/com/signalfx/lambda/example/CustomHandler.java).

### Step 4: Locate ingest endpoint

By default, this function wrapper will send data to the us0 realm. As a result, if you are not in us0 realm and you want to use the ingest endpoint directly, then you must explicitly set your realm. To set your realm, use a subdomain, such as ingest.us1.signalfx.com or ingest.eu0.signalfx.com.

To locate your realm:

1. Open SignalFx and in the top, right corner, click your profile icon.
2. Click **My Profile**.
3. Next to **Organizations**, review the listed realm.

### Step 5: Set environment variables

Set the Lambda environment variables as follows:

1) Set authentication token:
    ```
     SIGNALFX_AUTH_TOKEN=signalfx token
    ```
2) Optional parameters available:
    ```
     SIGNALFX_API_HOSTNAME=[pops.signalfx.com]
     SIGNALFX_API_PORT=[443]
     SIGNALFX_API_SCHEME=[https]
     SIGNALFX_SEND_TIMEOUT=milliseconds for signalfx client timeout [2000]
    ```

When setting SIGNALFX_API_HOSTNAME, remember to account for your realm, as explained in Step 4.

### Step 6: Send a custom metric from the Lambda function (optional)
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

### Step 7: For advanced users - reducing size of the deployment package with AWS Lambda Layers (optional)
You can reduce size of your deployment package by taking advantage of AWS Lambda Layers feature.
To learn more about Lambda Layers, please visit the AWS documentation site and see [AWS Lambda Layers](https://docs.aws.amazon.com/lambda/latest/dg/configuration-layers.html).

On a high level, there are 3 steps to leverage AWS Lambda Layer:
* Step 1: Locate the layer you wish to use
* Step 2: Make sure that the dependencies included in the layer are __not__ included in the your Lambda .jar file
* Step 3: Attach the layer to the Lambda function.

How you achieve Steps 2) and 3) depends on your build and deployment system. In general, one way to __not__  include the wrapper in your .jar file is to mark the depdendency as having `provided` scope.

For Step 1), SignalFx provides two ways to use its layer containing SignalFx Lambda wrapper:
#### Option 1: Hosted layer 
Use the version of the layer hosted by SignalFx. Hosted layers are available on per-region basis - to identify latest available version in your region, please see [the list of supported versions](https://github.com/signalfx/lambda-layer-versions/blob/master/java/JAVA.md).
Use the identified ARN in your deployment scripts/AWS console.
#### Option 2: SAM (Serverless Application Model) template
Deploy the copy of the SignalFx provided layer to your account. SignalFx provides a SAM template, which, when deployed, will create a layer with the wrapper in your AWS account.
To follow this Option, log in to your AWS Account. Go to Lambda --> Create a Function and choose the option to create a function from a template. Search for SignalFx, choose Java and deploy. 
Alternatively, locate the SignalFx layer using Serverless Application Repository service and deploy from there. 

## Additional information

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
| function_wrapper_version  | SignalFx function wrapper qualifier (e.g. signalfx-lambda-0.0.5) |
| metric_source | The literal value of 'lambda_wrapper' |

## Testing
Test example is available at `com.signalfx.lambda.example.CustomHandler::handler`. Make appropriate changes if needed.

### Testing locally
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

Apache Software License v2. Copyright © 2014-2020 SignalFx
