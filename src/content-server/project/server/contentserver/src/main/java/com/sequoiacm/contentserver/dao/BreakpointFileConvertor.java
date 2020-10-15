package com.sequoiacm.contentserver.dao;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
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
        ScmContentServer contentServer = ScmContentServer.getInstance();
        try {
            ScmFileOperateUtils.checkDirForCreateFile(wsInfo, parentId);
            contentServer.getMetaService().breakpointFileToFile(wsInfo, breakpointFile, fileInfo);
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
