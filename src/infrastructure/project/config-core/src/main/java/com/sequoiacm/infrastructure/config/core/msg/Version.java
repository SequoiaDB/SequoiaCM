package com.sequoiacm.infrastructure.config.core.msg;

import org.bson.BSONObject;

public interface Version {
    public int getVersion();

    public String getBussinessName();

    public BSONObject toBSONObject();
}
