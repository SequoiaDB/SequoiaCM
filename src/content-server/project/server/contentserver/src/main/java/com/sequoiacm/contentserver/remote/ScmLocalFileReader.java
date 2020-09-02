package com.sequoiacm.contentserver.remote;

import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.exception.ScmError;

public class ScmLocalFileReader extends ScmFileReader {
    private ScmDataReader reader = null;

    public ScmLocalFileReader(int siteId, ScmWorkspaceInfo wsInfo, ScmDataInfo dataInfo)
            throws ScmServerException {
        try {
            reader = ScmDataOpFactoryAssit.getFactory().createReader(
                    ScmContentServer.getInstance().getLocalSite(),
                    wsInfo.getName(), wsInfo.getDataLocation(),
                    ScmContentServer.getInstance().getDataService(), dataInfo);
        } catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_READ_ERROR),
                    "Failed to create data reader", e);
        }
    }

    @Override
    public int read(byte[] buff, int offset, int len) throws ScmServerException {
        try {
            return reader.read(buff, offset, len);
        } catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_READ_ERROR),
                    "Failed to read data", e);
        }
    }

    @Override
    public void seek(long size) throws ScmServerException {
        try {
            reader.seek(size);
        } catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_READ_ERROR),
                    "Failed to seek data", e);
        }
    }

    @Override
    public void close() {
        if (null != reader) {
            reader.close();
            reader = null;
        }
    }

    @Override
    public boolean isEof() {
        return reader.isEof();
    }

    @Override
    public long getSize() {
        return reader.getSize();
    }
}