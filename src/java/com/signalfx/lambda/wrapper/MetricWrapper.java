/*
 * Copyright (C) 2017 SignalFx, Inc.
 */
package com.signalfx.lambda.wrapper;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfx.metrics.connection.HttpEventProtobufReceiverFactory;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.signalfx.metrics.flush.AggregateMetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * @author park
 */
public class MetricWrapper implements Closeable {

    protected static final String AUTH_TOKEN = "SIGNALFX_AUTH_TOKEN";
    private static final String TIMEOUT_MS = "SIGNALFX_SEND_TIMEOUT";

    // metric names
    protected static final String METRIC_NAME_PREFIX = "aws.lambda.";
    protected static final String METRIC_NAME_INVOCATIONS = METRIC_NAME_PREFIX + "invocations";
    protected static final String METRIC_NAME_COLD_STARTS = METRIC_NAME_PREFIX + "coldStarts";
    protected static final String METRIC_NAME_ERRORS = METRIC_NAME_PREFIX + "errors";
    protected static final String METRIC_NAME_DURATION = METRIC_NAME_PREFIX + "duration";

    private final AggregateMetricSender.Session session;

    private final List<SignalFxProtocolBuffers.Dimension> defaultDimensions;

    private static boolean isColdStart = true;

    private final long startTime;

    public MetricWrapper(Context context) {
        this(context, null);
    }

    public MetricWrapper(Context context,
                         List<SignalFxProtocolBuffers.Dimension> dimensions) {
        String authToken = System.getenv(AUTH_TOKEN);
        int timeoutMs = -1;
        try {
            timeoutMs = Integer.valueOf(System.getenv(TIMEOUT_MS));
        } catch (NumberFormatException e) {
            // use default
        }

        // Create endpoint for ingest URL
        SignalFxReceiverEndpoint signalFxEndpoint = new SignalFxEndpoint();

        // Create datapoint dataPointReceiverFactory for endpoint
        HttpDataPointProtobufReceiverFactory dataPointReceiverFactory = new HttpDataPointProtobufReceiverFactory(signalFxEndpoint)
                .setVersion(2);

        HttpEventProtobufReceiverFactory eventReceiverFactory = new HttpEventProtobufReceiverFactory(
                signalFxEndpoint);

        if (timeoutMs > -1) {
            dataPointReceiverFactory.setTimeoutMs(timeoutMs);
            eventReceiverFactory.setTimeoutMs(timeoutMs);
        }

        AggregateMetricSender metricSender = new AggregateMetricSender("",
                dataPointReceiverFactory,
                eventReceiverFactory,
                new StaticAuthToken(authToken),
                Collections.<OnSendErrorHandler> singleton(metricError -> {
                    context.getLogger().log("Metric sending error");
                }));
        session = metricSender.createSession();

        this.defaultDimensions = getDefaultDimensions(context).entrySet().stream().map(
                e -> getDimensionAsProtoBuf(e.getKey(), e.getValue())
        ).collect(Collectors.toList());

        if (dimensions != null) {
            this.defaultDimensions.addAll(dimensions);
        }

        MetricSender.setWrapper(this);

        startTime = System.nanoTime();
        sendMetricCounter(METRIC_NAME_INVOCATIONS, SignalFxProtocolBuffers.MetricType.COUNTER);
        if (isColdStart) {
            isColdStart = false;
            sendMetricCounter(METRIC_NAME_COLD_STARTS, SignalFxProtocolBuffers.MetricType.COUNTER);
        }
    }

    public static Map<String, String> getDefaultDimensions(Context context) {
        Map<String, String> defaultDimensions = new HashMap<>();
        String functionArn = context.getInvokedFunctionArn();
        String[] splitted = functionArn.split(":");
        if ("lambda".equals(splitted[2])) {
            defaultDimensions.put("aws_function_name", context.getFunctionName());
            defaultDimensions.put("aws_function_version", context.getFunctionVersion());
            // only add if it's lambda arn
            // formatting is per specification at http://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html#arn-syntax-lambda
            defaultDimensions.put("lambda_arn", functionArn);
            defaultDimensions.put("aws_region", splitted[3]);
            defaultDimensions.put("aws_account_id", splitted[4]);
            if ("function".equals(splitted[5]) && splitted.length == 8) {
                defaultDimensions.put("aws_function_qualifier", splitted[7]);
            } else if ("event-source-mappings".equals(splitted[5]) && splitted.length > 6) {
                defaultDimensions.put("event_source_mappings", splitted[6]);
            }
        }
        return defaultDimensions;
    }

    private static SignalFxProtocolBuffers.Dimension getDimensionAsProtoBuf(String key, String value){
        return SignalFxProtocolBuffers.Dimension.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();
    }

    private void sendMetric(String metricName, SignalFxProtocolBuffers.MetricType metricType,
                            SignalFxProtocolBuffers.Datum datum) {
        SignalFxProtocolBuffers.DataPoint.Builder builder =
                SignalFxProtocolBuffers.DataPoint.newBuilder()
                        .setMetric(metricName)
                        .setMetricType(metricType)
                        .setValue(datum);
        MetricSender.sendMetric(builder);
    }

    private void sendMetricCounter(String metricName,
                                   SignalFxProtocolBuffers.MetricType metricType) {
        sendMetric(metricName, metricType,
                SignalFxProtocolBuffers.Datum.newBuilder().setIntValue(1).build());
    }

    protected void sendMetric(SignalFxProtocolBuffers.DataPoint.Builder builder) {
        builder.addAllDimensions(defaultDimensions);
        session.setDatapoint(builder.build());
    }

    public void error() {
        sendMetricCounter(METRIC_NAME_ERRORS, SignalFxProtocolBuffers.MetricType.COUNTER);
    }

    @Override
    public void close() throws IOException {
        sendMetric(METRIC_NAME_DURATION, SignalFxProtocolBuffers.MetricType.GAUGE,
                SignalFxProtocolBuffers.Datum.newBuilder()
                    .setDoubleValue((System.nanoTime() - startTime) / 1000000f)
                    .build()
        );
        session.close();
    }
}
