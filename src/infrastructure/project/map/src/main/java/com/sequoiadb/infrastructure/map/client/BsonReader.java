package com.sequoiadb.infrastructure.map.client;

import java.io.Closeable;

import org.bson.BSONObject;

import com.sequoiadb.infrastructure.map.ScmMapServerException;

public interface BsonReader extends Closeable {

    boolean hasNext();

    BSONObject getNext() throws ScmMapServerException;

    @Override
    void close();
}
