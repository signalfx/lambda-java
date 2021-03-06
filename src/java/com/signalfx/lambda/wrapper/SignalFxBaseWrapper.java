/*
 * Copyright (C) 2017 SignalFx, Inc.
 */
package com.signalfx.lambda.wrapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.base.Strings;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

/**
 * @author park
 */
abstract class SignalFxBaseWrapper {

    protected Object targetObject;
    protected Class<?> targetClass;
    protected String targetMethodName;
    protected Method targetMethod;

    protected static final String SIGNALFX_LAMBDA_HANDLER = "SIGNALFX_LAMBDA_HANDLER";

    protected void instantiateTargetClass(MetricWrapper wrapper) {
        String functionSpec = System.getenv(SIGNALFX_LAMBDA_HANDLER);
        if (Strings.isNullOrEmpty(functionSpec)) {
            throw new RuntimeException(SIGNALFX_LAMBDA_HANDLER + " was not specified.");
        }

        // expect format to be package.ClassName::methodName
        String[] splitted = functionSpec.split("::");
        if (splitted.length != 2) {
            throw new RuntimeException(functionSpec + " is not a valid handler name. Expected format: package.ClassName::methodName");
        }
        String handlerClassName = splitted[0];
        targetMethodName = splitted[1];

        try {
            targetClass = Class.forName(handlerClassName);
            Constructor<?> ctor = targetClass.getConstructor();
            targetObject = ctor.newInstance();

        } catch (ClassNotFoundException e) {
            // no class found
            throw new RuntimeException(handlerClassName + " not found in classpath");
        } catch (NoSuchMethodException e) {
            // no constructor found
            throw new RuntimeException(handlerClassName + " does not have an appropriate constructor");
        } catch (InstantiationException e) {
            // it's a call to an abstract class
            throw new RuntimeException(handlerClassName + " cannot be an abstract class");
        } catch (IllegalAccessException e) {
            // non accessible access to instantiate
            throw new RuntimeException(handlerClassName + "'s constructor is not accessible");
        } catch (InvocationTargetException e) {
            // constructor throws exception
            wrapper.error();
            throw new RuntimeException(handlerClassName + " threw an exception from the constructor");
        }
    }
}
