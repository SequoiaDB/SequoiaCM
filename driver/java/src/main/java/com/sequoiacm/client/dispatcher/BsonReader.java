package com.sequoiacm.client.dispatcher;

import com.sequoiacm.client.exception.ScmException;
import org.bson.BSONObject;

import java.io.Closeable;

public interface BsonReader extends Closeable {

    boolean hasNext();

    BSONObject getNext() throws ScmException;

    @Override
    void close();
}
