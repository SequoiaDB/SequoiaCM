package com.sequoiacm.config.framework.common;

import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public interface IDGeneratorDao {

    int getNewId(String type, InitIdCallback callback) throws ScmConfigException;
}