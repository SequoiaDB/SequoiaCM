package com.sequoiacm.deploy.parser;

import org.bson.BSONObject;

public interface ConfCoverter<T> {
    T convert(BSONObject bson);
}
