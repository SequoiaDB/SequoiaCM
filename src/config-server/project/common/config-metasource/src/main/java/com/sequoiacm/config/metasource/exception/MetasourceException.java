package com.sequoiacm.config.metasource.exception;

import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public class MetasourceException extends ScmConfigException {

    public MetasourceException(String msg, Throwable cause) {
        super(ScmConfError.METASOURCE_ERROR, msg, cause);
    }

    public MetasourceException(ScmConfError error, String msg, Throwable cause) {
        super(error, msg, cause);
    }
    
    public MetasourceException(ScmConfError error, String msg) {
        super(error, msg);
    }

}
