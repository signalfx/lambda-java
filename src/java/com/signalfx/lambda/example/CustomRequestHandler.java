/*
 * Copyright (C) 2017 SignalFx, Inc.
 */
package com.signalfx.lambda.example;

import java.io.IOException;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signalfx.lambda.wrapper.MetricSender;
import com.signalfx.lambda.wrapper.MetricWrapper;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * @author park
 */
public class CustomRequestHandler implements RequestHandler<Map<String, String>, String> {

    @Override
    public String handleRequest(Map<String, String> input, Context context) {
        try (MetricWrapper wrapper = new MetricWrapper(context)) {

            ObjectMapper objectMapper = new ObjectMapper();
            String str = objectMapper.writeValueAsString(input);
            System.out.println(str);

            SignalFxProtocolBuffers.DataPoint.Builder builder =
                    SignalFxProtocolBuffers.DataPoint.newBuilder()
                            .setMetric("application.metric")
                            .setMetricType(SignalFxProtocolBuffers.MetricType.GAUGE)
                            .setValue(
                                    SignalFxProtocolBuffers.Datum.newBuilder()
                                            .setDoubleValue(Math.random() * 100));
            builder.addDimensionsBuilder().setKey("applicationName").setValue("CoolApp").build();
            MetricSender.sendMetric(builder);
        } catch (IOException e) {
            // ignore
        }
        return "done";
    }
}
