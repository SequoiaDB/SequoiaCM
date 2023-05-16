package com.sequoiacm.contentserver.dao;

import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.FileMetaOperator;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaFactory;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileDeletorDao {
    private static final Logger logger = LoggerFactory.getLogger(FileDeletorDao.class);

    @Autowired
    private FileOperationListenerMgr listenerMgr;
    @Autowired
    private BucketInfoManager bucketInfoMgr;
    @Autowired
    private FileMetaOperator fileMetaOperator;

    @Autowired
    private FileAddVersionDao addVersionDao;

    @Autowired
    private FileMetaFactory fileMetaFactory;

    public FileMeta delete(String sessionId, String userDetail, ScmWorkspaceInfo wsInfo,
            String fileId, boolean isPhysical)
            throws ScmServerException {
        if (!isPhysical) {
            // 目前指定ID不支持非物理删除，报错
            throw new ScmOperationUnsupportedException(
                    "delete file by id only support physical delete: ws=" + wsInfo.getName()
                            + ", fileId=" + fileId);
        }
        ScmFileDeletorPysical fileDeleter = new ScmFileDeletorPysical(sessionId, userDetail, wsInfo,
                fileId, listenerMgr, fileMetaOperator);
        fileDeleter.delete();
        return null;
    }

    // 如果删除产生了新版本（deleteMarker），返回这个版本的元数据，否则返回 null
    public FileMeta delete(String sessionId, String userDetail, String username, ScmBucket bucket,
            String fileName, boolean isPhysical)
            throws ScmServerException {
        if (!isPhysical) {
            ScmFileDeleterWithVersionControl fileDeleter = new ScmFileDeleterWithVersionControl(
                    sessionId, userDetail, username, bucket, fileName, listenerMgr, bucketInfoMgr,
                    fileMetaOperator, addVersionDao, fileMetaFactory);
            return fileDeleter.delete();
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
            ScmFileDeletorPysical fileDeleter = new ScmFileDeletorPysical(sessionId, userDetail,
                    ScmContentModule.getInstance()
                            .getWorkspaceInfoCheckLocalSite(bucket.getWorkspace()),
                    BsonUtils.getStringChecked(file, FieldName.BucketFile.FILE_ID), listenerMgr,
                    fileMetaOperator);
            fileDeleter.delete();
            return null;
        }
        throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                "file not exist for physical delete: bucket=" + bucket.getName() + ", fileName="
                        + fileName);
    }
}
