package com.sequoiacm.contentserver.common;

import org.bson.BSONObject;

public interface BsonConverter<T> {
    BSONObject convert(T value);
    T convert(BSONObject object);
}
