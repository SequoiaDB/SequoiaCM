package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmSeekableDataWriter;
import com.sequoiacm.datasource.metadata.sequoiadb.SdbDataLocation;
import com.sequoiacm.sequoiadb.SequoiadbException;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;

public class SdbSeekableDataWriter extends SdbBreakpointDataWriter
        implements ScmSeekableDataWriter {

    public SdbSeekableDataWriter(SdbDataLocation sdbLocation, SdbDataService sds, String csName,
            String clName, String dataId, boolean createData, long writeOffset)
            throws SequoiadbException {
        super(sdbLocation, sds, csName, clName, dataId, createData, writeOffset);
    }

    @Override
    public void write(byte[] data, int offset, int length) throws SequoiadbException {
        super.seekWrite(data, offset, length);
    }

    @Override
    public void seek(long size) throws ScmDatasourceException {
        super.seek(size);
    }
}
