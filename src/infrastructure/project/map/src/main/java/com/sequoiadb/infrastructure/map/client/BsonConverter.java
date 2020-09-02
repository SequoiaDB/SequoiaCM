package com.sequoiadb.infrastructure.map.client;

import org.bson.BSONObject;

import com.sequoiadb.infrastructure.map.ScmMapServerException;

public interface BsonConverter<T> {
    T convert(BSONObject obj) throws ScmMapServerException;
}