package com.sequoiacm.config.framework.common;

import com.sequoiacm.config.metasource.exception.MetasourceException;

public interface InitIdCallback {

    int getInitId() throws MetasourceException;
}
