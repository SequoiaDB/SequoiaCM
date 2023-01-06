package com.sequoiacm.contentserver.dao;

import java.util.Date;

import org.springframework.util.StringUtils;

import com.sequoiacm.contentserver.datasourcemgr.ScmDataOpFactoryAssit;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.common.ScmDataWriterContext;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmBreakpointDataWriter;
import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public class BreakpointFileDeleter {

    private ScmWorkspaceInfo workspaceInfo;
    private BreakpointFile file;

    public BreakpointFileDeleter(ScmWorkspaceInfo wsInfo, BreakpointFile file)
            throws ScmServerException {
        this.workspaceInfo = wsInfo;
        this.file = file;

    }

    public void delete() throws ScmServerException {
        ScmContentModule.getInstance().getMetaService().deleteBreakpointFile(file);
        if (!StringUtils.hasText(file.getDataId())) {
            return;
        }

        if (file.isCompleted()) {
            deleteCompleteFile();
            return;
        }

        try {
            abortBreakpointFile();
        }
        catch (ScmDatasourceException e) {
            if (e.getScmError(ScmError.DATA_ERROR) == ScmError.DATA_NOT_EXIST) {
                deleteCompleteFile();
            }
            throw new ScmServerException(ScmError.DATA_DELETE_ERROR,
                    "failed to delete breakpoint data: ws=" + workspaceInfo.getName()
                            + ", breakpointFile=" + file,
                    e);
        }
        catch (ScmServerException e) {
            throw new ScmServerException(ScmError.DATA_DELETE_ERROR,
                    "failed to delete breakpoint data: ws=" + workspaceInfo.getName()
                            + ", breakpointFile=" + file,
                    e);
        }
    }

    private void abortBreakpointFile() throws ScmServerException, ScmDatasourceException {
        ScmBreakpointDataWriter writer = ScmDataOpFactoryAssit.getFactory().createBreakpointWriter(
                workspaceInfo.getDataLocation(file.getWsVersion()),
                ScmContentModule.getInstance().getDataService(), workspaceInfo.getName(),
                file.getFileName(), file.getDataId(), new Date(file.getCreateTime()), false,
                file.getUploadSize(), file.getExtraContext(), new ScmDataWriterContext());
        try {
            writer.abort();
        }
        finally {
            writer.close();
        }

    }

    private void deleteCompleteFile() throws ScmServerException {
        ScmDataInfo dataInfo = ScmDataInfo.forOpenExistData(ENDataType.Normal.getValue(),
                file.getDataId(),
                new Date(file.getCreateTime()), file.getWsVersion(), file.getTableName());

        ScmDataDeletor dataDeleter;
        try {
            dataDeleter = ScmDataOpFactoryAssit.getFactory().createDeletor(
                    ScmContentModule.getInstance().getLocalSite(), file.getWorkspaceName(),
                    workspaceInfo.getDataLocation(dataInfo.getWsVersion()),
                    ScmContentModule.getInstance().getDataService(), dataInfo);
            dataDeleter.delete();
        }
        catch (ScmDatasourceException e) {
            throw new ScmServerException(ScmError.DATA_DELETE_ERROR,
                    "failed to delete breakpoint data: ws=" + workspaceInfo.getName()
                            + ", breakpointFile=" + file,
                    e);
        }
    }
}
