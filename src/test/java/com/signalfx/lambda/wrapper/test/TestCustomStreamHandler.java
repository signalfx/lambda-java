/*
 * Copyright (C) 2017 SignalFx, Inc.
 */
package com.signalfx.lambda.wrapper.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

/**
 * @author park
 */
public class TestCustomStreamHandler implements RequestStreamHandler {

    public static final String CORRECT_INPUT = "correctInput";
    public static final String CORRECT_OUTPUT = "correctOutput";

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context)
            throws IOException {
        String actualInput = IOUtils.toString(input);
        if (!CORRECT_INPUT.equals(actualInput)) {
            throw new RuntimeException("Input is in correct. Expected: " +
                    CORRECT_INPUT + " but got: " + actualInput);
        }
        output.write(CORRECT_OUTPUT.getBytes());
        output.close();
    }
}
