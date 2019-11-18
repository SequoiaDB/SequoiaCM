package com.sequoiacm.config.framework.lock;

import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public class ScmLockException extends ScmConfigException {

    public ScmLockException(String msg) {
        super(ScmConfError.LOCK_ERROR, msg);
    }

    public ScmLockException(String msg, Throwable cause) {
        super(ScmConfError.LOCK_ERROR, msg, cause);
    }
}
