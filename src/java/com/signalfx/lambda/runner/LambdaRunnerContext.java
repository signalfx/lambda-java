/*
 * Copyright (C) 2017 SignalFx, Inc.
 */
package com.signalfx.lambda.runner;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

/**
 * @author park
 */
public class LambdaRunnerContext implements Context {

    @Override
    public String getAwsRequestId() {
        return "someRandomId";
    }

    @Override
    public String getLogGroupName() {
        return "logGroupName";
    }

    @Override
    public String getLogStreamName() {
        return "logStreamName";
    }

    @Override
    public String getFunctionName() {
        return "TestFunctionName";
    }

    @Override
    public String getFunctionVersion() {
        return "$LATEST";
    }

    @Override
    public String getInvokedFunctionArn() {
        return "arn:aws:lambda:us-east-1:123456789012:function:LambdaRunnerTest:1.234";
    }

    @Override
    public CognitoIdentity getIdentity() {
        return null;
    }

    @Override
    public ClientContext getClientContext() {
        return null;
    }

    @Override
    public int getRemainingTimeInMillis() {
        return 0;
    }

    @Override
    public int getMemoryLimitInMB() {
        return 0;
    }

    @Override
    public LambdaLogger getLogger() {
        return System.out::println;
    }
}
