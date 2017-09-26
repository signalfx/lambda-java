/*
 * Copyright (C) 2017 SignalFx, Inc.
 */
package com.signalfx.lambda.wrapper;

import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * @author park
 */
public class SignalFxRequestStreamWrapper extends SignalFxBaseWrapper implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        try (MetricWrapper wrapper = new MetricWrapper(context)) {
            long startTime = System.nanoTime();
            sendMetric(METRIC_NAME_INVOCATION, SignalFxProtocolBuffers.MetricType.COUNTER, 1);
            if (targetClass == null) {
                instantiateTargetClass();

                // assume cold start
                sendMetric(METRIC_NAME_COLD_START, SignalFxProtocolBuffers.MetricType.COUNTER, 1);
            }

            if (!(targetObject instanceof RequestStreamHandler)) {
                throw new RuntimeException(targetClass.getName() + " is not an instance of RequestStreamHandler");
            }

            try {
                ((RequestStreamHandler) targetObject).handleRequest(input, output, context);
            } catch (Exception e) {
                // Underlying method throw exception
                sendMetric(METRIC_NAME_ERROR, SignalFxProtocolBuffers.MetricType.COUNTER, 1);
                throw e;
            } finally {
                sendMetric(METRIC_NAME_COMPLETE, SignalFxProtocolBuffers.MetricType.COUNTER, 1);
                sendMetric(METRIC_NAME_DURATION, SignalFxProtocolBuffers.MetricType.GAUGE,
                        System.nanoTime() - startTime);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
