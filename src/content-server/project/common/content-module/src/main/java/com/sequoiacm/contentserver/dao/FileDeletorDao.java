package com.sequoiacm.contentserver.dao;

import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.metasource.ScmMetasourceException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
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
    private final String sessionId;
    private final String userDetail;
    private final FileOperationListenerMgr listenerMgr;
    private final BucketInfoManager bucketInfoMgr;

    private ScmFileDeletor fileDelete = null;

    public FileDeletorDao(String sessionId, String userDetail, FileOperationListenerMgr listenerMgr,
            BucketInfoManager bucketInfoMgr) {
        this.sessionId = sessionId;
        this.userDetail = userDetail;
        this.listenerMgr = listenerMgr;
        this.bucketInfoMgr = bucketInfoMgr;
    }

    public void init(ScmWorkspaceInfo wsInfo, String fileId, boolean isPhysical)
            throws ScmServerException {
        if (!isPhysical) {
            throw new ScmOperationUnsupportedException(
                    "delete file by id only support physical delete: ws=" + wsInfo.getName()
                            + ", fileId=" + fileId);
        }
        fileDelete = new ScmFileDeletorPysical(sessionId, userDetail, wsInfo, fileId, listenerMgr,
                bucketInfoMgr);
    }

    public void init(String username, ScmBucket bucket, String fileName, boolean isPhysical)
            throws ScmServerException {
        if (!isPhysical) {
            fileDelete = new ScmFileDeleterWithVersionControl(sessionId, userDetail, username, bucket,
                    fileName, listenerMgr, bucketInfoMgr);
            return;
        }
        BSONObject file;
        try {
            file = bucket.getFileTableAccessor(null).queryOne(
                    new BasicBSONObject(FieldName.BucketFile.FILE_NAME, fileName), null, null);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to get file info: bucket="
                    + bucket.getName() + ", fileName=" + fileName, e);
        }
        if (file != null) {
            fileDelete = new ScmFileDeletorPysical(sessionId, userDetail,
                    ScmContentModule.getInstance()
                            .getWorkspaceInfoCheckExist(bucket.getWorkspace()),
                    BsonUtils.getStringChecked(file, FieldName.BucketFile.FILE_ID), listenerMgr,
                    bucketInfoMgr);
            return;
        }
        throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                "file not exist for physical delete: bucket=" + bucket.getName() + ", fileName="
                        + fileName);
    }


    // 如果删除产生了新版本（deleteMarker），返回这个版本的元数据，否则返回 null
    public BSONObject delete() throws ScmServerException {
        return fileDelete.delete();
    }
}
