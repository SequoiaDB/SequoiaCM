package com.sequoiacm.infrastructure.lock.exception;

public class ZkPacketLenOutOfRangeException extends Exception {

    public ZkPacketLenOutOfRangeException(String message) {
        super(message);
    }

    public ZkPacketLenOutOfRangeException(Throwable cause) {
        super(cause);
    }

    public ZkPacketLenOutOfRangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
