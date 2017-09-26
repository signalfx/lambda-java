/*
 * Copyright (C) 2017 SignalFx, Inc.
 */
package com.signalfx.lambda.wrapper;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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

    private static final String AUTH_TOKEN = "SIGNALFX_AUTH_TOKEN";
    private static final String TIMEOUT_MS = "SIGNALFX_SEND_TIMEOUT";

    private final AggregateMetricSender.Session session;

    private final List<SignalFxProtocolBuffers.Dimension> defaultDimensions = new LinkedList<>();

    public MetricWrapper(Context context) {
        String authToken = System.getenv(AUTH_TOKEN);
        int timeoutMs = -1;
        try {
            timeoutMs = Integer.valueOf(System.getenv(TIMEOUT_MS));
        } catch (NumberFormatException e) {
            // use default
        }

        String functionArn = context.getInvokedFunctionArn();

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

        String[] splitted = functionArn.split(":");
        if ("lambda".equals(splitted[2])) {
            // only add if it's lambda arn
            // formatting is per specification at http://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html#arn-syntax-lambda
            defaultDimensions.add(getDimensionAsProtoBuf("lambda_arn", functionArn));
            defaultDimensions.add(getDimensionAsProtoBuf("aws_region", splitted[3]));
            defaultDimensions.add(getDimensionAsProtoBuf("aws_account_id", splitted[4]));
            if ("function".equals(splitted[5])) {
                defaultDimensions.add(getDimensionAsProtoBuf("aws_function_name", splitted[6]));
                String functionVersion;
                if (splitted.length == 8) {
                    functionVersion = splitted[7];
                } else {
                    functionVersion = context.getFunctionVersion();
                }

                defaultDimensions.add(getDimensionAsProtoBuf("aws_function_version",
                        functionVersion));
            } else if ("event-source-mappings".equals(splitted[5])) {
                defaultDimensions.add(getDimensionAsProtoBuf("event_source_mappings", splitted[6]));
            }
        }

        MetricSender.setWrapper(this);
    }

    private static SignalFxProtocolBuffers.Dimension getDimensionAsProtoBuf(String key, String value){
        return SignalFxProtocolBuffers.Dimension.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();
    }

    protected void sendMetric(SignalFxProtocolBuffers.DataPoint.Builder builder) {
        builder.addAllDimensions(defaultDimensions);
        session.setDatapoint(builder.build());
    }

    @Override
    public void close() throws IOException {
        session.close();
    }
}
