package com.sequoiacm.contentserver.dao;

import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.model.BreakpointFile;
import org.springframework.util.StringUtils;

import java.util.Date;

public class BreakpointFileDeleter {

    private ScmWorkspaceInfo workspaceInfo;
    private BreakpointFile file;
    private ScmDataDeletor dataDeleter;

    public BreakpointFileDeleter(ScmWorkspaceInfo wsInfo,
                                 BreakpointFile file) throws ScmServerException {
        this.workspaceInfo = wsInfo;
        this.file = file;

        if (StringUtils.hasText(file.getDataId())) {
            ScmDataInfo dataInfo = new ScmDataInfo(
                    ENDataType.Normal.getValue(),
                    file.getDataId(),
                    new Date(file.getCreateTime()));
            try {
                dataDeleter = ScmDataOpFactoryAssit.getFactory().createDeletor(
                        ScmContentServer.getInstance().getLocalSite(), file.getWorkspaceName(),
                        wsInfo.getDataLocation(), ScmContentServer.getInstance().getDataService(),
                        dataInfo);
            } catch (ScmDatasourceException e) {
                throw new ScmServerException(e.getScmError(ScmError.DATA_DELETE_ERROR),
                        "Failed to create data deleter", e);
            }
        }
    }

    public void delete() throws ScmServerException {
        ScmContentServer.getInstance().getMetaService().deleteBreakpointFile(file);
        if (dataDeleter != null) {
            try {
                dataDeleter.delete();
            } catch (ScmDatasourceException e) {
                throw new ScmServerException(e.getScmError(ScmError.DATA_DELETE_ERROR),
                        "Failed to delete breakpoint data", e);
            }
        }
    }
}
