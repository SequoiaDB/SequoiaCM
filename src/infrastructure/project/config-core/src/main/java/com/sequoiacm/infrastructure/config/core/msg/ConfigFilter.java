package com.sequoiacm.infrastructure.config.core.msg;

import org.bson.BSONObject;

public interface ConfigFilter {
    BSONObject toBSONObject();
}
