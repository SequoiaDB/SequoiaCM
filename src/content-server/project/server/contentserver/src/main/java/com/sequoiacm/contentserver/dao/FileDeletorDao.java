package com.sequoiacm.contentserver.dao;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public class FileDeletorDao {
    private static final Logger logger = LoggerFactory.getLogger(FileDeletorDao.class);

    private String fileId = null;
    private String fileName = null;

    private ScmFileDeletor fileDelete = null;

    public FileDeletorDao() throws ScmServerException {

    }

    public void init(String sessionId, String userDetail, ScmWorkspaceInfo wsInfo, String fileId,
            int majorVersion, int minorVersion, boolean isPhysical,
            FileOperationListenerMgr listenerMgr) throws ScmServerException {
        if (!isPhysical) {
            throw new ScmOperationUnsupportedException("support physical delete only!");
        }

        fileDelete = new ScmFileDeletorPysical(sessionId, userDetail, wsInfo, fileId, majorVersion,
                minorVersion, listenerMgr);
        this.fileId = fileId;
        getFileName(wsInfo, fileId, majorVersion, minorVersion);
    }

    private void getFileName(ScmWorkspaceInfo wsInfo, String fileId, int majorVersion,
            int minorVersion) throws ScmServerException {
        ScmContentModule contentModule = ScmContentModule.getInstance();
        BSONObject file = contentModule.getCurrentFileInfo(wsInfo, fileId);
        if (file == null) {
            throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                    "file not found:ws=" + wsInfo.getName() + ",fileId=" + fileId + ",version="
                            + majorVersion + "." + minorVersion);
        }
        this.fileName = (String) file.get(FieldName.FIELD_CLFILE_NAME);
    }

    public String getFileName() {
        return fileName;
    }

    public void delete() throws ScmServerException {
        try {
            fileDelete.delete();
        }
        catch (ScmServerException e) {
            if (e.getError() != ScmError.FILE_NOT_FOUND) {
                logger.error("delete file failed:fileId=" + fileId);
                throw e;
            }
            // file is not exist, return ok.
        }
    }
}
