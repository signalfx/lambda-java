/*
 * Copyright (C) 2017 SignalFx, Inc.
 */
package com.signalfx.lambda.wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import com.signalfx.lambda.runner.LambdaRunnerContext;
import com.signalfx.lambda.wrapper.test.TestCustomHandler;
import com.signalfx.lambda.wrapper.test.TestCustomStreamHandler;

/**
 * @author park
 */

public class WrapperTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void testStreamWrapper() throws Exception {
        environmentVariables.set(SignalFxBaseWrapper.SIGNALFX_LAMBDA_HANDLER, "com.signalfx.lambda.wrapper.test.TestCustomStreamHandler::handler");

        InputStream inputStream = new ByteArrayInputStream(TestCustomStreamHandler.CORRECT_INPUT.getBytes(StandardCharsets.UTF_8.name()));
        OutputStream outputStream = new ByteArrayOutputStream();

        SignalFxRequestStreamWrapper streamWrapper = new SignalFxRequestStreamWrapper();
        streamWrapper.handleRequest(inputStream, outputStream, new LambdaRunnerContext());
    }

    @Test
    public void testRequestWrapper() throws Exception {
        environmentVariables.set(SignalFxBaseWrapper.SIGNALFX_LAMBDA_HANDLER, "com.signalfx.lambda.wrapper.test.TestCustomHandler::handler");

        SignalFxRequestWrapper streamWrapper = new SignalFxRequestWrapper();
        Object response = streamWrapper
                .handleRequest(TestCustomHandler.CORRECT_INPUT, new LambdaRunnerContext());
        assertEquals(TestCustomHandler.CORRECT_OUTPUT, response);
    }

    @Test
    public void testRequestWrapperWithNoVersionInContext() throws Exception {
        environmentVariables.set(SignalFxBaseWrapper.SIGNALFX_LAMBDA_HANDLER, "com.signalfx.lambda.wrapper.test.TestCustomHandler::handler");

        SignalFxRequestWrapper streamWrapper = new SignalFxRequestWrapper();
        Object response = streamWrapper
                .handleRequest(TestCustomHandler.CORRECT_INPUT, new LambdaRunnerContext() {

                    @Override
                    public String getInvokedFunctionArn() {
                        return "arn:aws:lambda:us-east-2:someAccountId:function:LambdaRunnerTest";
                    }
                });
        assertEquals(TestCustomHandler.CORRECT_OUTPUT, response);
    }

    @Test
    public void testRequestWrapperException() throws Exception {
        environmentVariables.set(SignalFxBaseWrapper.SIGNALFX_LAMBDA_HANDLER, "com.signalfx.lambda.wrapper.test.TestCustomHandler::handlerException");

        SignalFxRequestWrapper streamWrapper = new SignalFxRequestWrapper();
        try {
            streamWrapper.handleRequest(TestCustomHandler.CORRECT_INPUT, new LambdaRunnerContext());
            fail("Expect handleRequest to throw exception");
        } catch (Exception e) {
            assertEquals(TestCustomHandler.EXCEPTION_OUTPUT, e.getCause().getMessage());
        }
    }

    @Test
    public void testRequestWrapperClassNotFoundException() throws Exception {
        environmentVariables.set(SignalFxBaseWrapper.SIGNALFX_LAMBDA_HANDLER, "com.signalfx.lambda.wrapper.test.TestCustomHandlerNotThere::handlerException");

        SignalFxRequestWrapper streamWrapper = new SignalFxRequestWrapper();
        try {
            streamWrapper.handleRequest(TestCustomHandler.CORRECT_INPUT, new LambdaRunnerContext());
            fail("Expect handleRequest to throw exception");
        } catch (Exception e) {
            // expected
        }
    }
}
