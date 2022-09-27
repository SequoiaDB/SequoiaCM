package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmSeekableDataWriter;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.sequoiadb.SequoiadbException;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;

public class SdbSeekableDataWriter extends SdbBreakpointDataWriter
        implements ScmSeekableDataWriter {

    public SdbSeekableDataWriter(SdbDataLocation sdbLocation, SdbDataService sds,
            MetaSource metaSource, String csName, String clName, String wsName, String dataId,
            boolean createData, long writeOffset, ScmLockManager lockManager)
            throws ScmDatasourceException {
        super(sdbLocation, sds, metaSource, csName, clName, wsName, dataId, createData, writeOffset,
                lockManager);
    }

    @Override
    @SlowLog(operation = "writeData")
    public void write(byte[] data, int offset, int length) throws SequoiadbException {
        super.seekWrite(data, offset, length);
    }

    @Override
    public void seek(long size) throws ScmDatasourceException {
        super.seek(size);
    }
}
