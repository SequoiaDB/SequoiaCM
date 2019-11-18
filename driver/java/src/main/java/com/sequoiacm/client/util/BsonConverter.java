package com.sequoiacm.client.util;

import com.sequoiacm.client.exception.ScmException;
import org.bson.BSONObject;

public interface BsonConverter<T> {
    T convert(BSONObject obj) throws ScmException;
}
