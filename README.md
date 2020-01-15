# SignalFx Java Lambda Wrapper

## Overview

You can use this document to add a SignalFx wrapper to your AWS Lambda for Java, specifically for Java 8+.

The SignalFx Java Lambda Wrapper wraps around an AWS Lambda Java function handler, which allows metrics to be sent to SignalFx.


## Step 1: Install via maven dependency

1. Run the following command: 

```xml
<dependency>
  <groupId>com.signalfx.public</groupId>
  <artifactId>signalfx-lambda</artifactId>
  <version>0.0.6</version>
</dependency>
```

## Step 2: Package and upload 

1. Package the .jar file, and then upload to AWS. 
  * To learn how to upload, please see the [instructions from AWS](http://docs.aws.amazon.com/lambda/latest/dg/java-create-jar-pkg-maven-no-ide.html).

## Step 3: Wrap the function

There are two ways to wrap the function. You can use the SignalFx handler or manually wrap the function. 

### (Recommended) Option 1: Use the SignalFx handler

1. Configure the AWS function handler to have one of the following values:

  * Use `com.signalfx.lambda.wrapper.SignalFxRequestWrapper::handleRequest` for normal input/output request. 
    * Please review the [example here](https://github.com/signalfx/lambda-java/blob/master/src/java/com/signalfx/lambda/example/CustomRequestHandler.java). 
  * Use `com.signalfx.lambda.wrapper.SignalFxRequestStreamWrapper::handleRequest` for normal stream request. 
    * Please review the [example here](https://github.com/signalfx/lambda-java/blob/master/src/java/com/signalfx/lambda/example/CustomStreamHandler.java).

For additional custom function signatures, see [additional examples](https://github.com/signalfx/lambda-java/tree/master/src/java/com/signalfx/lambda/example). 

2. Use the SIGNALFX_LAMBDA_HANDLER environment variable to set the handler function. The format of the handler needs to be `package.ClassName::methodName`, such as `com.signalfx.lambda.example.CustomHandler::handler`. 

Review the following example. 
```
SIGNALFX_LAMBDA_HANDLER=com.signalfx.lambda.example.CustomHandler::handler
```

### Option 2: Manually wrap the function

1. Review the following example of manually wrapping a function.

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
2. For additional custom function signatures, review the [custom handler example](https://github.com/signalfx/lambda-java/blob/master/src/java/com/signalfx/lambda/example/CustomHandler.java).

## Step 4: Locate ingest endpoint

By default, this function wrapper will send data to the us0 realm. As a result, if you are not in the us0 realm and you want to use the ingest endpoint directly, then you must explicitly set your realm. 

To locate your realm:

1. Open SignalFx and in the top, right corner, click your profile icon.
2. Click **My Profile**.
3. Next to **Organizations**, review the listed realm.

To set your realm, you will need to use a subdomain, such as ingest.us1.signalfx.com or ingest.eu0.signalfx.com. This action will be explained in Step 5.  

## Step 5: Set Lambda environment variables

1. Set authentication token:
    ```
     SIGNALFX_AUTH_TOKEN=signalfx token
    ```
2. (Optional) Set additional parameters:  
    ```
     SIGNALFX_API_HOSTNAME=[pops.signalfx.com]
     SIGNALFX_API_PORT=[443]
     SIGNALFX_API_SCHEME=[https]
     SIGNALFX_SEND_TIMEOUT=milliseconds for signalfx client timeout [2000]
    ```

If you set SIGNALFX_API_HOSTNAME, remember to use your correct realm, as mentioned in Step 4.

## (Optional) Step 6: Send custom metrics from the Lambda function 

1. To send custom metrics, review the following example. 

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

## (Optional) Step 7: Reduce the size of deployment packages with AWS Lambda Layers

For advanced users who want to reduce the size of deployment packages, please visit the AWS documentation site and see [AWS Lambda Layers](https://docs.aws.amazon.com/lambda/latest/dg/configuration-layers.html).

At a high level, to reduce the size of deployments with AWS Lambda layers, you must: 

1. Determine the layer to use. There are two options: 
  * Option 1: Layer hosted by SignalFx
    * You can use the version of the layer hosted by SignalFx. Available hosted layers may differ based on region. To review the latest available version based on your region, please see [the list of supported versions](https://github.com/signalfx/lambda-layer-versions/blob/master/java/JAVA.md).
  * Option 2: SAM (Serverless Application Model) template
    * You can deploy a copy of the SignalFx-provided layer to your account. SignalFx provides a SAM template that will create a layer with the wrapper in your AWS account. 
    * To use this option, log into your AWS account. In the Lambda section, create a function, and then choose the option to create a function from a template. Search for SignalFx, choose Java, and then deploy. 
    * You can also locate the SignalFx layer using the Serverless Application Repository service. 
2. Verify that dependencies included in the layer are __not__ included in the Lambda .jar file.
3. Attach the layer to the Lambda function.

Based on your build and deployment system, there are various ways to complete Step 2 and Step 3. In general, to **not** include the wrapper in your .jar file, you can mark the dependency as having `provided` scope.  

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

### Test
1. A test example is available at `com.signalfx.lambda.example.CustomHandler::handler`. If necessary, make desired changes. 

### Test locally
1. Set test input event and Lambda function handler. Review the following example. 
    ```
    LAMBDA_INPUT_EVENT={"abc": "def"}
    SIGNALFX_LAMBDA_HANDLER=com.signalfx.lambda.example.CustomHandler::handler
    ```
2. Run `mvn compile exec:java`.

### Test from the AWS Console
1. Run `mvn clean compile package -Ptest` to package using the test profile, which will include the runner and test handler.

2. Set the SignalFx Lambda handler environment variable to either
`com.signalfx.lambda.example.CustomHandler::handler` or `com.signalfx.lambda.example.CustomStreamHandler::handleRequest`.

## License

Apache Software License v2. Copyright © 2014-2020 SignalFx
