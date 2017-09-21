/*
 * Copyright (C) 2017 SignalFx, Inc.
 */
package com.signalfx.lambda.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

/**
 * @author park
 */
public class CustomStreamHandler implements RequestStreamHandler {
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context)
            throws IOException {
        System.out.println(IOUtils.toString(input));
        output.write("this is right".getBytes());
        output.close();
    }
}
