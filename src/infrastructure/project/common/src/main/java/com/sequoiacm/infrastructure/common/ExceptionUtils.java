package com.sequoiacm.infrastructure.common;

import java.net.SocketTimeoutException;

public class ExceptionUtils {

    public static boolean causedBySocketTimeout(Throwable e) {
        if (e == null) {
            return false;
        }
        if (e instanceof SocketTimeoutException) {
            return true;
        }
        return causedBySocketTimeout(e.getCause());
    }
}
