# SignalFx Java Lambda Wrapper

SignalFx Java Lambda Wrapper.

## Supported Languages

* Java 7+

## Usage

SignalFx Java Lambda Wrapper is a wrapper to instrument execution of Java Lambda as well sending metrics to SignalFx inside AWS Lambda.

### Install via maven dependency:
```xml
<dependency>
  <groupId>com.signalfx.public</groupId>
  <artifactId>signalfx-lambda</artifactId>
  <version>0.0.1</version>
</dependency>
```

###  Package
Package jar file and upload to AWS per [instructions here](http://docs.aws.amazon.com/lambda/latest/dg/java-create-jar-pkg-maven-no-ide.html)

### Handler
Set handler to
- `com.signalfx.lambda.wrapper.SignalFxRequestWrapper::handleRequest` for normal Input/Output request
- `com.signalfx.lambda.wrapper.SignalFxRequestStreamWrapper::handleRequest` for normal Stream request

### Environment Variable
Set the Lambda environment variable as followed:

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
 SIGNALFX_SEND_TIMEOUT=milliseconds for signalfx client timeout [2000]

### Sending metric from lambda
```java
// construct data point builder
SignalFxProtocolBuffers.DataPoint.Builder builder =
        SignalFxProtocolBuffers.DataPoint.newBuilder()
                .setMetric("application.metric")
                .setMetricType(SignalFxProtocolBuffers.MetricType.GAUGE)
                .setValue(
                        SignalFxProtocolBuffers.Datum.newBuilder()
                                .setDoubleValue(100));

// add any additional dimension
builder.addDimensionsBuilder().setKey("applicationName").setValue("CoolApp").build();

// send the metric
MetricSender.sendMetric(builder);
```

## Testing
Test example is available at `com.signalfx.lambda.example.CustomHandler::handler`. Make appropriate changes if needed.

### Testing locally.
1) Set test input event and lambda function handler
```
LAMBDA_INPUT_EVENT={"abc":"def"}
SIGNALFX_LAMBDA_HANDLER=com.signalfx.lambda.TestCustomHandler::handler
```
2) run `mvn compile exec:java`

### Testing from the AWS Console
1)Run `mvn clean compile package -Ptest` to package using test profile, which will include runner and test handler.

2) Set the signalfx lambda handler environment variable to either
`com.signalfx.lambda.example.CustomHandler::handler` or `com.signalfx.lambda.example.CustomStreamHandler::handleRequest`

## License

Apache Software License v2. Copyright © 2014-2017 SignalFx