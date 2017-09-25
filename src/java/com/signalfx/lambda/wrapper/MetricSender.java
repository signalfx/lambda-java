/*
 * Copyright (C) 2017 SignalFx, Inc.
 */
package com.signalfx.lambda.wrapper;

import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * @author park
 */
public class MetricSender {

    private static MetricWrapper singleton;

    public static void sendMetric(SignalFxProtocolBuffers.DataPoint.Builder builder) {
        if (singleton != null) {
            singleton.sendMetric(builder);
        }
    }

    protected static void setWrapper(MetricWrapper metricWrapper) {
        singleton = metricWrapper;
    }
}
