package com.sequoiacm.common.checksum;

public class ChecksumException extends Exception {

    public ChecksumException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChecksumException(String message) {
        super(message);
    }
}
