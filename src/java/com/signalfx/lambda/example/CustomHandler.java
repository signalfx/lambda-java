/*
 * Copyright (C) 2017 SignalFx, Inc.
 */
package com.signalfx.lambda.example;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signalfx.lambda.wrapper.MetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * @author park
 */
public class CustomHandler {

    public String handler(Map<String, String> input) {
        throw new RuntimeException("this is wrong");
    }

    public String handler(Map<String, String> input, Context context)
            throws JsonProcessingException {
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
        MetricSender.sendMetric(builder);
        return "here";
    }
}
