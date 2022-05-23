package com.sequoiacm.mappingutil.exec;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

import java.io.Closeable;

public interface FileIdCursor extends Closeable {

    long getMarker();

    ScmId getNext() throws ScmToolsException;

    boolean hasNext() throws ScmToolsException;

    void close();
}
