package com.sequoiacm.test.parser;

import org.bson.BSONObject;

public interface ConfConverter<T> {

    T convert(BSONObject bson);
}
