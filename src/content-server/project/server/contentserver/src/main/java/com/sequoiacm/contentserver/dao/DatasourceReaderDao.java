package com.sequoiacm.contentserver.dao;

import java.io.IOException;
import java.io.OutputStream;

import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.datasource.dataoperation.ScmDataReader;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.common.Const;

public class DatasourceReaderDao {
    private ScmDataReader reader;
    private ScmWorkspaceInfo wsInfo;
    private ScmDataInfo dataInfo;

    public DatasourceReaderDao(ScmWorkspaceInfo wsInfo, ScmDataInfo dataInfo)
            throws ScmServerException {
        this.wsInfo = wsInfo;
        this.dataInfo = dataInfo;
        try {
            this.reader = ScmDataOpFactoryAssit.getFactory().createReader(
                    ScmContentServer.getInstance().getLocalSite(), wsInfo.getName(),
                    wsInfo.getDataLocation(), ScmContentServer.getInstance().getDataService(),
                    dataInfo);
        } catch (ScmDatasourceException e) {
            throw new ScmServerException(e.getScmError(ScmError.DATA_READ_ERROR),
                    "Failed to create data reader", e);
        }
    }

    public void read(OutputStream os) throws ScmServerException {
        byte[] buf = new byte[Const.TRANSMISSION_LEN];
        while (true) {
            int actLen = 0;
            try {
                actLen = reader.read(buf, 0, Const.TRANSMISSION_LEN);
            } catch (ScmDatasourceException e) {
                throw new ScmServerException(e.getScmError(ScmError.DATA_READ_ERROR),
                        "Failed to read data", e);
            }
            if (actLen == -1) {
                break;
            }
            try {
                os.write(buf, 0, actLen);
            }
            catch (IOException e) {
                throw new ScmServerException(ScmError.NETWORK_IO,
                        "write data to outputstream failed:ws=" + wsInfo.getName() + ",dataInfo="
                                + dataInfo,
                                e);
            }
        }
    }

    public void close() {
        reader.close();
    }

    public long getSize() {
        return reader.getSize();
    }
}
