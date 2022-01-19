package com.sequoiacm.contentserver.dao;

import org.bson.BSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.infrastructure.lock.ScmLock;

public class BreakpointFileConvertor implements IFileCreatorDao {
    private ScmWorkspaceInfo wsInfo;
    private BreakpointFile breakpointFile;
    private BSONObject fileInfo;

    public BreakpointFileConvertor(ScmWorkspaceInfo wsInfo, BreakpointFile breakpointFile,
            BSONObject fileInfo) {
        this.wsInfo = wsInfo;
        this.breakpointFile = breakpointFile;
        this.fileInfo = fileInfo;
    }

    @Override
    public BSONObject insert() throws ScmServerException {
        String parentId = (String) fileInfo.get(FieldName.FIELD_CLFILE_DIRECTORY_ID);

        ScmLock rLock = ScmFileOperateUtils.lockDirForCreateFile(wsInfo, parentId);
        ScmContentModule contentModule = ScmContentModule.getInstance();
        try {
            ScmFileOperateUtils.checkDirForCreateFile(wsInfo, parentId);
            contentModule.getMetaService().breakpointFileToFile(wsInfo, breakpointFile, fileInfo);
        }
        finally {
            if (rLock != null) {
                rLock.unlock();
            }
        }
        return fileInfo;
    }

    // breakfile not rollback lob
    @Override
    public void processException() {
        return;
    }

    @Override
    public String getWorkspaceName() {
        return wsInfo.getName();
    }
}
