package com.sequoiacm.infrastructure.common;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

public enum ScmParameterCheckEnum {

    CircuitBreakerEnabled("hystrix.command.default.circuitBreaker.enabled", new CheckIsBoolean()),
    BreakerRequestVolumeThreshold("hystrix.command.default.circuitBreaker.requestVolumeThreshold", new CheckIsNaturalNumberAndRange(
            1, Integer.MAX_VALUE)),
    CircuitBreakerErrorThresholdPercentage("hystrix.command.default.circuitBreaker.errorThresholdPercentage", new CheckIsNaturalNumberAndRange(
            1, 100)),
    CircuitBreakerSleepWindowInMilliseconds("hystrix.command.default.circuitBreaker.sleepWindowInMilliseconds", new CheckIsNaturalNumberAndRange(
            1, Integer.MAX_VALUE)),
    ExecutionIsolationSemaphoreMaxConcurrentRequests("hystrix.command.default.execution.isolation.semaphore.maxConcurrentRequests", new CheckIsNaturalNumberAndRange(
            1, Integer.MAX_VALUE));

    private final String key;

    private final ScmParameterValidate validate;

    ScmParameterCheckEnum(String key, ScmParameterValidate validate) {
        this.key = key;
        this.validate = validate;
    }

    public CheckParameterResult doValidate(String key, String value) {
        return validate.validate(key, value);
    }

    public String getKey() {
        return key;
    }

}

abstract class ScmParameterValidate {
    abstract CheckParameterResult validate(String key, String value);

    CheckParameterResult isNaturalNumber(String key, String value) {
        if (StringUtils.isBlank(value)) {
            return new CheckParameterResult(false, "The value of " + key + " is "
                    + (StringUtils.isEmpty(value) ? "null" : value) + "," + "cannot be empty");
        }
        Pattern pattern = Pattern.compile("[0-9]*");
        if (pattern.matcher(value).matches()) {
            return new CheckParameterResult(true, "check successful");
        }
        else {
            return new CheckParameterResult(false, "The value of " + key + " is " + value + ","
                    + value + " is not a natural number");
        }
    }
}

class CheckIsBoolean extends ScmParameterValidate {
    @Override
    CheckParameterResult validate(String key, String value) {
        if ("false".equals(value) || "true".equals(value)) {
            return new CheckParameterResult(true, "check successful");
        }
        else {
            return new CheckParameterResult(false,
                    "The value of " + key + " is " + (StringUtils.isEmpty(value) ? "null" : value)
                            + "," + (StringUtils.isEmpty(value) ? "null" : value)
                            + " is not a boolean type");
        }
    }
}

class CheckIsNaturalNumber extends ScmParameterValidate {
    @Override
    CheckParameterResult validate(String key, String value) {
        return this.isNaturalNumber(key, value);
    }
}

class CheckIsNaturalNumberAndRange extends ScmParameterValidate {

    private int minValue;
    private int maxValue;

    public CheckIsNaturalNumberAndRange(int minValue, int maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    CheckParameterResult validate(String key, String value) {
        CheckParameterResult checkParameterResult = isNaturalNumber(key, value);
        if (!checkParameterResult.isSuccessful()) {
            return checkParameterResult;
        }
        int numValue = Integer.parseInt(value);
        if (minValue > numValue || maxValue < numValue) {
            return new CheckParameterResult(false,
                    "The value of " + key + " is " + value + "," + value
                            + " is not in the range of [" + minValue + "," + maxValue + "]");
        }
        return new CheckParameterResult(true, "check successful");
    }
}