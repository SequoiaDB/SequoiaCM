package com.sequoiacm.client.exception;

import com.sequoiacm.exception.ScmError;

public class ScmException extends Exception {

    private static final long serialVersionUID = 2254727849190358010L;

    private ScmError error;
    private int errcode;
    private String requestUrl; // nullable

    /**
     * Use this to specify the error object, error message and exception chaining.
     * @param error The enumeration object of sequoiacm error.
     * @param message The error message.
     * @param cause The exception used to build exception chain.
     */
    public ScmException(ScmError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
        this.errcode = error.getErrorCode();
    }

    /**
     * Use this to specify the error object and error message.
     * @param error The enumeration object of sequoiacm error.
     * @param message The error message.
     */
    public ScmException(ScmError error, String message) {
        super(message);
        this.error = error;
        this.errcode = error.getErrorCode();
    }

    /**
     * Use this to specify the error code and error message.
     * @param errorCode The error code return by content server.
     * @param message The error message.
     */
    public ScmException(int errorCode, String message) {
        super(message);
        this.error = ScmError.getScmError(errorCode);
        this.errcode = errorCode;
    }

    /**
     * Use this to specify the error code and error message.
     * 
     * @param errorCode
     *            The error code return by content server.
     * @param message
     *            The error message.
     * @param requestUrl
     *            The request url.
     */
    public ScmException(int errorCode, String message, String requestUrl) {
        super(message);
        this.error = ScmError.getScmError(errorCode);
        this.errcode = errorCode;
        this.requestUrl = requestUrl;
    }

    /**
     * Returns the error object.
     * @return The enumeration object of sequoiacm error.
     */
    public ScmError getError() {
        return error;
    }

    /**
     * Returns the error code.
     * @return The error code.
     */
    public int getErrorCode() {
        return errcode;
    }

    /**
     * Returns the error type.
     * @return The error type.
     */
    public String getErrorType() {
        return error.getErrorType();
    }

    /**
     * Returns a description of the exception.
     * @return The exception description.
     */
    public String toString() {
        String res = String.format("%s, error=%s(%d): %s",
                super.toString(), error.name(), errcode, error.getErrorDescription());
        if (requestUrl != null) {
            res = res + ", requestUrl=" + requestUrl;
        }
        return res;
    }
}
