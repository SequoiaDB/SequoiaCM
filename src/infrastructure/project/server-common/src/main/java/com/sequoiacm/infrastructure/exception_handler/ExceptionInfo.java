package com.sequoiacm.infrastructure.exception_handler;

import java.util.Map;

/**
 * expection info:include body ,header
 */
public class ExceptionInfo {
    private ExceptionBody exceptionBody;
    private Map<String, String> extraExceptionHeader;

    public ExceptionInfo(ExceptionBody exceptionBody, Map<String, String> extraExceptionHeader) {
        this.exceptionBody = exceptionBody;
        this.extraExceptionHeader = extraExceptionHeader;
    }

    public ExceptionInfo() {
    }

    public ExceptionBody getExceptionBody() {
        return exceptionBody;
    }

    public void setExceptionBody(ExceptionBody exceptionBody) {
        this.exceptionBody = exceptionBody;
    }

    public Map<String, String> getExtraExceptionHeader() {
        return extraExceptionHeader;
    }

    public void setExtraExceptionHeader(Map<String, String> extraExceptionHeader) {
        this.extraExceptionHeader = extraExceptionHeader;
    }
}
