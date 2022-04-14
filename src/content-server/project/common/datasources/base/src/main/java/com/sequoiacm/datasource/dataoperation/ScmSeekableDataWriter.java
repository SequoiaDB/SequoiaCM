package com.sequoiacm.datasource.dataoperation;

import com.sequoiacm.datasource.ScmDatasourceException;

public interface ScmSeekableDataWriter extends ScmBreakpointDataWriter {
    void seek(long size) throws ScmDatasourceException;
}
