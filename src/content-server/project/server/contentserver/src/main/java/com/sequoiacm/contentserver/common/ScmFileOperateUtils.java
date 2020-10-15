package com.sequoiacm.contentserver.common;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaService;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.MetaRelAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;

public class ScmFileOperateUtils {
    public static ScmLock lockDirForCreateFile(ScmWorkspaceInfo wsInfo, String parentId)
            throws ScmServerException {
        if (wsInfo.isEnableDirectory()
                && !parentId.equals(CommonDefine.Directory.SCM_ROOT_DIR_ID)) {
            ScmLockPath lockPath = ScmLockPathFactory.createDirLockPath(wsInfo.getName(), parentId);
            return ScmLockManager.getInstance().acquiresReadLock(lockPath);
        }
        return null;
    }

    public static void checkDirForCreateFile(ScmWorkspaceInfo wsInfo, String parentId)
            throws ScmServerException {
        if (wsInfo.isEnableDirectory()) {
            ScmMetaService metaservice = ScmContentServer.getInstance().getMetaService();
            BSONObject parentDirMatcher = new BasicBSONObject();
            parentDirMatcher.put(FieldName.FIELD_CLDIR_ID, parentId);
            if (metaservice.getDirCount(wsInfo.getName(), parentDirMatcher) <= 0) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "parent directory not exists:preantDirectoryId=" + parentId);
            }
        }
    }

    public static void insertFileRelForCreateFile(ScmWorkspaceInfo wsInfo, BSONObject fileInfo,
            TransactionContext context) throws ScmServerException, ScmMetasourceException {
        if (wsInfo.isEnableDirectory()) {
            BSONObject relInsertor = ScmMetaSourceHelper.createRelInsertorByFileInsertor(fileInfo);
            MetaRelAccessor relAccessor = ScmContentServer.getInstance().getMetaService()
                    .getMetaSource().getRelAccessor(wsInfo.getName(), context);
            relAccessor.insert(relInsertor);
        }
    }

    public static void updateFileRelForUpdateFile(ScmWorkspaceInfo wsInfo, String fileId,
            BSONObject oldFileRecord, BSONObject relUpdator, TransactionContext context)
            throws ScmServerException, ScmMetasourceException {
        if (wsInfo.isEnableDirectory()) {
            MetaRelAccessor relAccessor = ScmContentServer.getInstance().getMetaService()
                    .getMetaSource().getRelAccessor(wsInfo.getName(), context);
            String oldDirId = BsonUtils.getStringChecked(oldFileRecord,
                    FieldName.FIELD_CLFILE_DIRECTORY_ID);
            String oldFileName = BsonUtils.getStringChecked(oldFileRecord,
                    FieldName.FIELD_CLFILE_NAME);
            relAccessor.updateRel(fileId, oldDirId, oldFileName, relUpdator);
        }
    }

    public static void deleteFileRelForDeleteFile(ScmWorkspaceInfo ws, String fileID,
            BSONObject deletedFileRecord, TransactionContext context)
            throws ScmServerException, ScmMetasourceException {
        if (ws.isEnableDirectory()) {
            MetaRelAccessor relAccessor = ScmContentServer.getInstance().getMetaService()
                    .getMetaSource().getRelAccessor(ws.getName(), context);
            String dirId = BsonUtils.getStringChecked(deletedFileRecord,
                    FieldName.FIELD_CLFILE_DIRECTORY_ID);
            String fileName = BsonUtils.getStringChecked(deletedFileRecord,
                    FieldName.FIELD_CLFILE_NAME);
            relAccessor.deleteRel(fileID, dirId, fileName);
        }
    }
}
