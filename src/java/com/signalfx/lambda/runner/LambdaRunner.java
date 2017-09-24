/*
 * Copyright (C) 2017 SignalFx, Inc.
 */
package com.signalfx.lambda.runner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

/**
 * LambdaRunner to run lambda locally.
 *
 * @author park
 */
public final class LambdaRunner<I, O> {

    public static <I, O> void main(final String[] args) throws
            Exception {

        String handlerClassName = System.getenv("LAMBDA_RUNNER_HANDLER");

        if (Strings.isNullOrEmpty(handlerClassName)) {
            throw new RuntimeException("LAMBDA_RUNNER_HANDLER environment variable must be set");
        }

        Context context = new LambdaRunnerContext();

        Object object;
        try {
            Class<?> clazz = Class.forName(handlerClassName);
            Constructor<?> ctor = clazz.getConstructor();
            object = ctor.newInstance();

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(handlerClassName + " not found in classpath");
        }

        if (object instanceof RequestHandler) {

            @SuppressWarnings("unchecked")
            RequestHandler<I, O> requestHandler = (RequestHandler<I, O>) object;
            I requestObject = getRequestObject(requestHandler);

            O output;
            try {
                output = requestHandler.handleRequest(requestObject, context);
            } catch (RuntimeException e) {
                System.out.println("FAIL:");
                e.printStackTrace();
                System.exit(1);
                return;
            }
            System.out.println("SUCCESS: " + (new ObjectMapper().writeValueAsString(output)));
        } else if (object instanceof RequestStreamHandler) {
            String input = System.getenv("LAMBDA_INPUT_EVENT");
            if (input == null) {
                input = "";
            }
            InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8.name()));
            OutputStream outputStream = new ByteArrayOutputStream();

            try {
                ((RequestStreamHandler)object).handleRequest(inputStream, outputStream, context);
            } catch (RuntimeException e) {
                System.out.println("FAIL:");
                e.printStackTrace();
                System.exit(1);
            }
            System.out.println("SUCCESS: " + outputStream.toString());
        } else {
            throw new RuntimeException("Request handler class does not implement " + RequestHandler.class + " or " + RequestStreamHandler.class +" interface");
        }

    }

    private static <I> I getRequestObject(RequestHandler handler) throws IOException {

        Type requestClass = null;

        for (Type genericInterface : handler.getClass().getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType) {
                Type[] genericTypes = ((ParameterizedType) genericInterface).getActualTypeArguments();
                requestClass = genericTypes[0];
            }
        }

        if (null == requestClass) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = System.getenv("LAMBDA_INPUT_EVENT");
            return mapper.readValue((String) json, mapper.getTypeFactory().constructType(requestClass));
        } catch (Exception e) {
            return mapper.readValue("{}", mapper.getTypeFactory().constructType(requestClass));
        }
    }
}