package com.sequoiacm.s3import.client.exception;

public class ScmS3ClientException extends Exception {

    public ScmS3ClientException(String errorMessage) {
        super(errorMessage);
    }

    public ScmS3ClientException(String message, Throwable t) {
        super(message, t);
    }
}
