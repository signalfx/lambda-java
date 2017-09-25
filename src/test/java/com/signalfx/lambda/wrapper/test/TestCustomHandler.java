/*
 * Copyright (C) 2017 SignalFx, Inc.
 */
package com.signalfx.lambda.wrapper.test;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signalfx.lambda.wrapper.MetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * @author park
 */
public class TestCustomHandler {

    public static final String CORRECT_INPUT = "correctInput";
    public static final String CORRECT_OUTPUT = "correctOutput";
    public static final String EXCEPTION_OUTPUT = "exceptionOutput";

    private void verifyInput(String input) {
        if (!CORRECT_INPUT.equals(input)) {
            throw new RuntimeException("Input is in correct. Expected: " +
                    CORRECT_INPUT + " but got: " + input);
        }

    }

    public String handlerException(String input) {
        verifyInput(input);
        throw new RuntimeException(EXCEPTION_OUTPUT);
    }

    public String handler(String input, Context context)
            throws JsonProcessingException {
        verifyInput(input);

        SignalFxProtocolBuffers.DataPoint.Builder builder =
                SignalFxProtocolBuffers.DataPoint.newBuilder()
                        .setMetric("application.metric")
                        .setMetricType(SignalFxProtocolBuffers.MetricType.GAUGE)
                        .setValue(
                                SignalFxProtocolBuffers.Datum.newBuilder()
                                        .setDoubleValue(Math.random() * 100));
        MetricSender.sendMetric(builder);
        return CORRECT_OUTPUT;
    }
}
