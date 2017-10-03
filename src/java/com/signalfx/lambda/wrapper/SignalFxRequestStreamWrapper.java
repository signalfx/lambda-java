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
public final class SignalFxRequestStreamWrapper extends SignalFxBaseWrapper implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        try (MetricWrapper wrapper = new MetricWrapper(context)) {
            if (targetClass == null) {
                instantiateTargetClass(wrapper);
            }

            if (!(targetObject instanceof RequestStreamHandler)) {
                throw new RuntimeException(targetClass.getName() + " is not an instance of RequestStreamHandler");
            }

            try {
                ((RequestStreamHandler) targetObject).handleRequest(input, output, context);
            } catch (Exception e) {
                // Underlying method throw exception
                wrapper.error();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
