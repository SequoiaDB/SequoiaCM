package com.sequoiacm.cloud.authentication.security;

import org.springframework.security.authentication.AccountStatusException;

/**
 * User passwordType is invalid.
 */
public class InvalidUserPasswordTypeException extends AccountStatusException {

    public InvalidUserPasswordTypeException(String msg) {
        super(msg);
    }

    public InvalidUserPasswordTypeException(String msg, Throwable e) {
        super(msg, e);
    }
}
