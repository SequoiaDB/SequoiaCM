package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.common.module.ScmBucketAttachFailure;
import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.ScmSystemUtils;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.dao.FileDeletorDao;
import com.sequoiacm.contentserver.dao.FileVersionDeleteDao;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.listener.OperationCompleteCallback;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.ObjectDeltaInfo;
import com.sequoiacm.contentserver.model.SessionInfoWrapper;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmDataInfoDetail;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.pipeline.file.module.FileExistStrategy;
import com.sequoiacm.contentserver.pipeline.file.module.FileMeta;
import com.sequoiacm.contentserver.pipeline.file.FileMetaOperator;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaDefaultUpdater;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaFactory;
import com.sequoiacm.contentserver.pipeline.file.module.FileMetaUpdater;
import com.sequoiacm.contentserver.pipeline.file.module.FileUploadConf;
import com.sequoiacm.contentserver.pipeline.file.module.UpdateFileMetaResult;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.quota.BucketQuotaManager;
import com.sequoiacm.contentserver.quota.QuotaInfo;
import com.sequoiacm.contentserver.service.IDatasourceService;
import com.sequoiacm.contentserver.service.IFileService;
import com.sequoiacm.contentserver.service.IScmBucketService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmObjectCursor;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.infrastructure.security.sign.SignUtil;
import com.sequoiacm.metasource.AllFileMetaCursor;
import com.sequoiacm.metasource.ContentModuleMetaSource;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sequoiacm.contentserver.bizconfig.ContenserverConfClient;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ScmBucketServiceImpl implements IScmBucketService {
    private static final Logger logger = LoggerFactory.getLogger(ScmBucketServiceImpl.class);

    @Autowired
    private ScmAudit audit;
    @Autowired
    private BucketInfoManager bucketInfoManager;

    @Autowired
    private FileOperationListenerMgr listenerMgr;

    @Autowired
    private IDatasourceService datasourceService;

    @Autowired
    private IFileService fileService;

    @Autowired
    private FileDeletorDao fileDeletorDao;

    @Autowired
    private FileVersionDeleteDao fileVersionDeleteDao;

    @Autowired
    private FileMetaOperator fileMetaOperator;

    @Autowired
    private BucketQuotaManager quotaManager;

    @Autowired
    private FileMetaFactory fileMetaFactory;

    @Override
    public ScmBucket createBucket(ScmUser user, String ws, String name) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, ws, ScmPrivilegeDefine.CREATE,
                "create bucket");
        ScmWorkspaceInfo workspaceInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckExist(ws);
        if (workspaceInfo.isEnableDirectory()) {
            throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                    "can not create bucket, please disable the workspace directory feature: ws="
                            + ws + ", bucket=" + name);
        }
        if (!ScmArgChecker.Bucket.checkBucketName(name)) {
            throw new ScmServerException(ScmError.INVALID_ARGUMENT, "invalid bucket name:" + name);
        }
        ScmBucket ret = ContenserverConfClient.getInstance().createBucket(user.getUsername(), ws,
                name);
        audit.info(ScmAuditType.CREATE_SCM_BUCKET, user, ws, 0,
                "create bucket: bucketName=" + name + ", ws=" + ws);
        return ret;
    }

    @Override
    public long countFile(ScmUser user, String bucketName, Integer scope, BSONObject condition,
            boolean isResContainsDeleteMarker) throws ScmServerException {
        ScmBucket bucket = bucketInfoManager.getBucket(bucketName);
        if (bucket == null) {
            throw new ScmServerException(ScmError.BUCKET_NOT_EXISTS,
                    "bucket not exist:" + bucketName);
        }
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.READ, "count bucket file");

        long ret = countFile(bucket, scope, condition, isResContainsDeleteMarker);
        audit.info(ScmAuditType.FILE_DQL, user, bucket.getWorkspace(), 0,
                "count file in bucket: bucketName=" + bucketName + ", ws=" + bucket.getWorkspace()
                        + ", condition=" + condition);
        return ret;
    }

    public long countFile(ScmBucket bucket, Integer scope, BSONObject condition,
            boolean isResContainsDeleteMarker) throws ScmServerException {
        try {
            if (scope == CommonDefine.Scope.SCOPE_CURRENT) {
                return getCurrentBucketFileCount(bucket, condition, isResContainsDeleteMarker);
            }

            ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                    .getWorkspaceInfoCheckLocalSite(bucket.getWorkspace());

            if (scope == CommonDefine.Scope.SCOPE_HISTORY) {
                return getHistoryBucketFileCount(wsInfo, bucket, condition,
                        isResContainsDeleteMarker);
            }

            if (scope == CommonDefine.Scope.SCOPE_ALL) {
                return getAllBucketFileCount(wsInfo, bucket, condition, isResContainsDeleteMarker);
            }
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to count file of the bucket: bucket="
                    + bucket.getName() + ", condition=" + condition, e);
        }

        throw new ScmInvalidArgumentException("unknown scope:scope=" + scope);
    }

    private long getCurrentBucketFileCount(ScmBucket bucket, BSONObject condition,
            boolean isResContainsDeleteMarker) throws ScmServerException, ScmMetasourceException {
        if (!isResContainsDeleteMarker) {
            condition = ScmMetaSourceHelper.generateNewMatcherWithNotDeleteMarker(condition);
        }

        MetaAccessor accessor = bucket.getFileTableAccessor(null);
        return accessor.count(condition);
    }

    private long getHistoryBucketFileCount(ScmWorkspaceInfo wsInfo, ScmBucket bucket,
            BSONObject condition, boolean isResContainsDeleteMarker) throws ScmServerException {
        BSONObject matcher = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_BUCKET_ID,
                bucket.getId());
        if (condition != null) {
            BasicBSONList arrayCond = new BasicBSONList();
            arrayCond.add(condition);
            arrayCond.add(matcher);

            matcher = new BasicBSONObject();
            matcher.put("$and", arrayCond);
        }
        return ScmContentModule.getInstance().getMetaService().getHistoryFileCount(
                wsInfo.getMetaLocation(), wsInfo.getName(), matcher, isResContainsDeleteMarker);
    }

    @Override
    public long getAllBucketFileCount(ScmWorkspaceInfo wsInfo, ScmBucket bucket,
            BSONObject condition, boolean isResContainsDeleteMarker)
            throws ScmServerException, ScmMetasourceException {
        long currentFileCount = getCurrentBucketFileCount(bucket, condition,
                isResContainsDeleteMarker);
        long historyBucketFileCount = getHistoryBucketFileCount(wsInfo, bucket, condition,
                isResContainsDeleteMarker);
        return currentFileCount + historyBucketFileCount;
    }

    @Override
    public ScmBucket getBucket(ScmUser user, String name) throws ScmServerException {
        ScmBucket bucket = getBucket(name);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.READ, "get bucket");
        return bucket;
    }

    @Override
    public ScmBucket getBucket(ScmUser user, long id) throws ScmServerException {
        ScmBucket bucket = getBucket(id);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.READ, "get bucket");
        return bucket;
    }

    @Override
    public ScmBucket getBucket(String name) throws ScmServerException {
        ScmBucket bucket = bucketInfoManager.getBucket(name);
        if (bucket == null) {
            throw new ScmServerException(ScmError.BUCKET_NOT_EXISTS, "bucket not exist:" + name);
        }
        return bucket;
    }

    @Override
    public ScmBucket getBucket(long id) throws ScmServerException {
        ScmBucket bucket = bucketInfoManager.getBucketById(id);
        if (bucket == null) {
            throw new ScmServerException(ScmError.BUCKET_NOT_EXISTS, "bucket not exist:" + id);
        }
        return bucket;
    }

    @Override
    public ScmBucket deleteBucket(ScmUser user, String name) throws ScmServerException {
        ScmBucket bucket = getBucket(name);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.DELETE, "delete bucket");

        if (countFile(bucket, CommonDefine.Scope.SCOPE_CURRENT, null, true) > 0) {
            throw new ScmServerException(ScmError.BUCKET_NOT_EMPTY, "bucket not empty:" + name);
        }
        try {
            ContenserverConfClient.getInstance().deleteBucket(name);
        }
        finally {
            bucketInfoManager.invalidateBucketCache(name);
        }
        audit.info(ScmAuditType.DELETE_SCM_BUCKET, user, bucket.getWorkspace(), 0,
                "delete bucket: bucketName=" + name);
        return bucket;
    }

    @Override
    public ScmObjectCursor<ScmBucket> listBucket(ScmUser user, BSONObject condition,
            BSONObject orderBy, long skip, long limit) throws ScmServerException {
        return ContenserverConfClient.getInstance().listBucket(condition, orderBy, skip, limit);
    }

    @Override
    public long countBucket(ScmUser user, BSONObject condition) throws ScmServerException {
        return ContenserverConfClient.getInstance().countBucket(condition);
    }

    @Override
    public FileMeta createFile(ScmUser user, String bucketName, FileMeta fileInfo, InputStream data,
            boolean isOverWrite) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        fileCreatePreCheck(bucket, user);

        fileInfo.setBucketId(bucket.getId());
        FileMeta ret = fileService.createFile(bucket.getWorkspace(), fileInfo,
                new FileUploadConf(
                        isOverWrite ? FileExistStrategy.OVERWRITE : FileExistStrategy.ADD_VERSION,
                        true),
                data);
        audit.info(ScmAuditType.CREATE_FILE, user, bucket.getWorkspace(), 0,
                "create file in bucket: bucketName=" + bucketName + ", fileId=" + ret.getId()
                        + ", fileName=" + ret.getName());
        return ret;

    }

    @Override
    public FileMeta createFile(ScmUser user, String bucketName, FileMeta fileInfo,
            String breakpointFile, boolean isOverWrite) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        fileCreatePreCheck(bucket, user);

        fileInfo.setBucketId(bucket.getId());
        FileMeta ret = fileService.createFile(bucket.getWorkspace(), fileInfo,
                new FileUploadConf(
                        isOverWrite ? FileExistStrategy.OVERWRITE : FileExistStrategy.ADD_VERSION,
                        true),
                breakpointFile);
        audit.info(ScmAuditType.CREATE_FILE, user, bucket.getWorkspace(), 0,
                "create file in bucket: bucketName=" + bucketName + ", fileId=" + ret.getId()
                        + ", fileName=" + ret.getName() + ", breakpointFile=" + breakpointFile);
        return ret;
    }

    @Override
    public FileMeta createFile(ScmUser user, String bucketName, FileMeta fileInfo,
            TransactionCallback transactionCallback, boolean isOverWrite)
            throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        fileCreatePreCheck(bucket, user);

        fileInfo.setBucketId(bucket.getId());
        FileMeta ret = fileService.createFile(bucket.getWorkspace(), fileInfo,
                new FileUploadConf(
                        isOverWrite ? FileExistStrategy.OVERWRITE : FileExistStrategy.ADD_VERSION,
                        true),
                transactionCallback);
        audit.info(ScmAuditType.CREATE_FILE, user, bucket.getWorkspace(), 0,
                "create file in bucket: bucketName=" + bucketName + ", fileId=" + ret.getId()
                        + ", fileName=" + ret.getName());
        return ret;
    }

    private void fileCreatePreCheck(ScmBucket bucket, ScmUser user) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.CREATE, "create file in bucket");
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckLocalSite(bucket.getWorkspace());
        if (wsInfo.isEnableDirectory()) {
            throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                    "can not create file in bucket, please disable the workspace directory feature: ws="
                            + wsInfo.getName() + ", bucket=" + bucket);
        }
    }

    private void deleteDataSilence(String ws, ScmDataInfoDetail dataDetail) {
        try {
            datasourceService.deleteData(ws, dataDetail.getDataInfo(), dataDetail.getSiteId());
        }
        catch (Exception e) {
            logger.warn("failed to delete data: ws={}, dataId={}, siteId={}", ws,
                    dataDetail.getDataInfo().getId(), dataDetail.getSiteId(), e);
        }
    }

    private static ScmLock lockBreakpointFile(String workspaceName, String breakpointFileName)
            throws ScmServerException {
        ScmLockPath lockPath = ScmLockPathFactory.createBPLockPath(workspaceName,
                breakpointFileName);
        return ScmLockManager.getInstance().acquiresLock(lockPath);
    }

    @Override
    public MetaCursor listFile(ScmUser user, String bucketName, Integer scope, BSONObject condition,
            BSONObject selector, BSONObject orderBy, long skip, long limit,
            boolean isResContainsDeleteMarker) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.READ, "list file in bucket");
        MetaCursor ret = listFile(scope, condition, selector, orderBy, skip, limit, bucket,
                isResContainsDeleteMarker);
        audit.info(ScmAuditType.FILE_DQL, user, bucket.getWorkspace(), 0,
                "list file in bucket: bucketName=" + bucketName + ", condition=" + condition
                        + ", orderby=" + orderBy + ", skip=" + skip + ", limit=" + limit);
        return ret;
    }

    private MetaCursor listFile(Integer scope, BSONObject condition, BSONObject selector,
            BSONObject orderBy, long skip, long limit, ScmBucket bucket,
            boolean isResContainsDeleteMarker) throws ScmServerException {
        try {
            if (scope == CommonDefine.Scope.SCOPE_CURRENT) {
                return queryCurrentBucketFile(bucket, condition, selector, orderBy, skip, limit,
                        isResContainsDeleteMarker);
            }

            ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                    .getWorkspaceInfoCheckLocalSite(bucket.getWorkspace());

            if (scope == CommonDefine.Scope.SCOPE_HISTORY) {
                return queryHistoryBucketFile(wsInfo, bucket, condition, selector, orderBy, skip,
                        limit, isResContainsDeleteMarker);
            }

            if (scope == CommonDefine.Scope.SCOPE_ALL) {
                if (!ScmSystemUtils.isEmptyBSONObject(orderBy) || skip != 0 || limit != -1) {
                    throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                            "query all file in bucket unsupport orderby/skip/limit");
                }
                return queryAllBucketFile(wsInfo, bucket, condition, selector, orderBy,
                        isResContainsDeleteMarker);
            }
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to list the file in the bucket, bucket=" + bucket.getName()
                            + ", condition=" + condition + ", orderby=" + orderBy + ", skip=" + skip
                            + ", limit=" + limit,
                    e);
        }

        throw new ScmInvalidArgumentException("unknown scope:scope=" + scope);
    }

    public MetaCursor queryCurrentBucketFile(ScmBucket bucket, BSONObject condition,
            BSONObject selector, BSONObject orderBy, long skip, long limit,
            boolean isResContainsDeleteMarker) throws ScmServerException, ScmMetasourceException {
        if (!isResContainsDeleteMarker) {
            condition = ScmMetaSourceHelper.generateNewMatcherWithNotDeleteMarker(condition);
        }

        MetaAccessor accessor = bucket.getFileTableAccessor(null);
        return accessor.query(condition, selector, orderBy, skip, limit, 0);
    }

    public MetaCursor queryHistoryBucketFile(ScmWorkspaceInfo wsInfo, ScmBucket bucket,
            BSONObject condition, BSONObject selector, BSONObject orderBy, long skip, long limit,
            boolean isResContainsDeleteMarker) throws ScmServerException, ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_BUCKET_ID,
                bucket.getId());
        if (condition != null) {
            BasicBSONList arrayCond = new BasicBSONList();
            arrayCond.add(condition);
            arrayCond.add(matcher);

            matcher = new BasicBSONObject();
            matcher.put("$and", arrayCond);
        }

        return ScmContentModule.getInstance().getMetaService().queryHistoryFile(
                wsInfo.getMetaLocation(), wsInfo.getName(), matcher, selector, orderBy, skip, limit,
                isResContainsDeleteMarker);
    }

    @Override
    public MetaCursor queryAllBucketFile(ScmWorkspaceInfo wsInfo, ScmBucket bucket,
            BSONObject matcher, BSONObject selector, BSONObject orderBy,
            boolean isResContainsDeleteMarker) throws ScmServerException, ScmMetasourceException {
        MetaCursor currentBucketFileCursor = null;
        MetaCursor historyBucketFileCursor = null;
        try {
            currentBucketFileCursor = queryCurrentBucketFile(bucket, matcher, selector, orderBy, 0,
                    -1, isResContainsDeleteMarker);
            historyBucketFileCursor = queryHistoryBucketFile(wsInfo, bucket, matcher, selector,
                    orderBy, 0, -1, isResContainsDeleteMarker);
            return new AllFileMetaCursor(currentBucketFileCursor, historyBucketFileCursor, orderBy);
        }
        catch (ScmMetasourceException e) {
            ScmSystemUtils.closeResource(currentBucketFileCursor);
            ScmSystemUtils.closeResource(historyBucketFileCursor);
            throw new ScmServerException(e.getScmError(),
                    String.format("Failed to query file in file table:fileMatcher=%s", matcher), e);
        }
        catch (Exception e) {
            ScmSystemUtils.closeResource(currentBucketFileCursor);
            ScmSystemUtils.closeResource(historyBucketFileCursor);
            throw new ScmSystemException(
                    String.format("Failed to query file in file table:fileMatcher=%s", matcher), e);
        }
    }

    public String getFileId(ScmBucket bucket, String fileName) throws ScmServerException {
        String bucketName = bucket.getName();
        try {
            MetaAccessor accessor = bucket.getFileTableAccessor(null);
            BSONObject bucketFileRel = accessor.queryOne(
                    new BasicBSONObject(FieldName.BucketFile.FILE_NAME, fileName), null, null);
            if (bucketFileRel == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "file not exist: bucket=" + bucketName + ", fileName=" + fileName);
            }
            return BsonUtils.getStringChecked(bucketFileRel, FieldName.BucketFile.FILE_ID);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to get file: bucket=" + bucketName + ", fileName=" + fileName, e);
        }
    }

    @Override
    public String getFileId(ScmUser user, String bucketName, String fileName)
            throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.READ, "get file in bucket");
        String fileId = getFileId(bucket, fileName);
        audit.info(ScmAuditType.FILE_DQL, user, bucket.getWorkspace(), 0,
                "get file in bucket: bucketName=" + bucketName + ", fileName=" + fileName
                        + ", fileId=" + fileId);
        return fileId;
    }

    @Override
    public List<ScmBucketAttachFailure> attachFile(ScmUser user, String bucketName,
            List<String> fileIdList, ScmBucketAttachKeyType type) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.CREATE, "bucket attach file");
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckLocalSite(bucket.getWorkspace());
        if (wsInfo == null) {
            throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST,
                    "bucket workspace not exist: bucket=" + bucketName + ",workspace="
                            + bucket.getWorkspace());
        }
        if (wsInfo.isEnableDirectory()) {
            throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                    "can not create file in bucket, please disable the workspace directory feature: ws="
                            + wsInfo.getName() + ", bucket=" + bucketName);
        }

        ContentModuleMetaSource metasource = ScmContentModule.getInstance().getMetaService()
                .getMetaSource();

        List<ScmBucketAttachFailure> ret = new ArrayList<>();
        for (String fileId : fileIdList) {
            ScmBucketAttachFailure failure = attachFile(bucket, wsInfo, metasource, fileId, type,
                    user.getUsername());
            if (failure != null) {
                ret.add(failure);
                continue;
            }
            audit.info(ScmAuditType.UPDATE_FILE, user, bucket.getWorkspace(), 0,
                    "bucket attach file: bucketName=" + bucketName + ", fileId=" + fileId);
        }
        return ret;
    }

    @Override
    public void detachFile(ScmUser user, String bucketName, String fileName)
            throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.CREATE, "bucket attach file");
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckLocalSite(bucket.getWorkspace());
        if (wsInfo == null) {
            throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST,
                    "bucket workspace not exist: bucket=" + bucketName + ",workspace="
                            + bucket.getWorkspace());
        }
        String fileId = getFileId(user, bucketName, fileName);

        ScmLock lock = ScmLockManager.getInstance()
                .acquiresWriteLock(ScmLockPathFactory.createFileLockPath(wsInfo.getName(), fileId));
        OperationCompleteCallback operationCompleteCallback = null;
        try {

            BSONObject file = ScmContentModule.getInstance().getCurrentFileInfo(wsInfo, fileId,
                    true);
            if (file == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "bucket file not found: bucket=" + bucketName + ", fileName=" + fileName
                                + ", fileId=" + fileId);
            }
            FileMeta fileMeta = fileMetaFactory.createFileMetaByRecord(wsInfo.getName(), file);
            if (!isFileInBucket(fileMeta, bucketName)) {
                logger.info(
                        "bucket file already detach from bucket: bucketName={}, fileName={}, fileId={}",
                        bucket.getName(), fileName, fileId);
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "bucket file not found: bucket=" + bucketName + ", fileName=" + fileName);
            }
            UpdateFileMetaResult res = fileMetaOperator.updateFileMeta(wsInfo.getName(), fileId,
                    Collections.singletonList(FileMetaDefaultUpdater
                            .globalFieldUpdater(FieldName.FIELD_CLFILE_FILE_BUCKET_ID, null)),
                    user.getUsername(), new Date(), fileMeta, null);
            operationCompleteCallback = listenerMgr.postUpdate(wsInfo, fileMeta,
                    res.getLatestVersionAfterUpdate());
            audit.info(ScmAuditType.UPDATE_FILE, user, bucket.getWorkspace(), 0,
                    "bucket detach file: bucketName=" + bucketName + ", fileName=" + fileName
                            + ", fileId=" + fileId);
        }
        finally {
            lock.unlock();
            if (operationCompleteCallback != null) {
                operationCompleteCallback.onComplete();
            }
        }
    }

    private boolean isFileInBucket(FileMeta fileMeta, String bucketName) throws ScmServerException {
        Long bucketId = fileMeta.getBucketId();
        if (bucketId == null) {
            return false;
        }
        ScmBucket bucket = bucketInfoManager.getBucketById(bucketId);
        if (bucket == null) {
            return false;
        }
        return bucket.getName().equals(bucketName);
    }

    @Override
    public ScmBucket updateBucketVersionStatus(ScmUser user, String bucketName,
            ScmBucketVersionStatus bucketVersionStatus) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.CREATE, "update bucket version");
        if (bucketVersionStatus == ScmBucketVersionStatus.Disabled) {
            throw new ScmServerException(ScmError.OPERATION_UNSUPPORTED,
                    "can not set bucket version to " + ScmBucketVersionStatus.Disabled + ", bucket="
                            + bucketName);
        }
        ScmBucket ret = ContenserverConfClient.getInstance()
                .updateBucketVersionStatus(user.getUsername(), bucketName, bucketVersionStatus);
        audit.info(ScmAuditType.UPDATE_SCM_BUCKET, user, bucket.getWorkspace(), 0,
                "update bucket version status: bucketName=" + bucketName + ", ws="
                        + bucket.getWorkspace() + ", versionStatus=" + bucketVersionStatus);
        return ret;
    }

    @Override
    public FileMeta deleteFile(ScmUser user, String bucketName, String fileName,
            boolean isPhysical, SessionInfoWrapper sessionInfoWrapper) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.DELETE, "delete bucket file");
        FileMeta ret = fileDeletorDao.delete(sessionInfoWrapper.getSessionId(),
                sessionInfoWrapper.getUserDetail(), user.getUsername(), bucket, fileName,
                isPhysical);
        audit.info(ScmAuditType.DELETE_FILE, user, bucket.getWorkspace(), 0,
                "delete file in bucket: bucketName=" + bucketName + ", fileName=" + fileName
                        + ", isPhysical=" + isPhysical);
        return ret;
    }

    @Override
    public FileMeta deleteFileVersion(ScmUser user, String bucketName, String fileName,
            int majorVersion, int minorVersion) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.DELETE, "delete bucket file version");
        FileMeta ret = fileVersionDeleteDao.delete(bucket, fileName, majorVersion, minorVersion);
        audit.info(ScmAuditType.DELETE_FILE, user, bucket.getWorkspace(), 0,
                "delete file version in bucket: bucketName=" + bucketName + ", fileName=" + fileName
                        + ", version=" + majorVersion + "." + minorVersion);
        return ret;
    }

    @Override
    public FileMeta deleteNullVersionFile(ScmUser user, String bucketName, String fileName)
            throws ScmServerException {
        return deleteFileVersion(user, bucketName, fileName, CommonDefine.File.NULL_VERSION_MAJOR,
                CommonDefine.File.NULL_VERSION_MINOR);
    }

    private BSONObject genFileAttachBucketUpdater(ScmBucket attachToBucket,
            ScmBucketAttachKeyType type, BSONObject fileRecord) {
        BSONObject newInfo = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_BUCKET_ID,
                attachToBucket.getId());
        String etag = BsonUtils.getString(fileRecord, FieldName.FIELD_CLFILE_FILE_ETAG);
        if (etag == null) {
            String md5 = BsonUtils.getString(fileRecord, FieldName.FIELD_CLFILE_FILE_MD5);
            if (md5 != null) {
                etag = SignUtil.toHex(md5);
            }
            else {
                // 被关联的文件没有md5属性，这里按分段上传形成的对象 ETAG 格式（这个 ETAG 不代表文件 MD5），造一个 ETAG 给这个文件
                String dataId = BsonUtils.getStringChecked(fileRecord,
                        FieldName.FIELD_CLFILE_FILE_DATA_ID);
                etag = SignUtil.calcHexMd5(dataId) + "-1";
            }
            newInfo.put(FieldName.FIELD_CLFILE_FILE_ETAG, etag);
        }

        if (type == ScmBucketAttachKeyType.FILE_ID) {
            // 关联类型是fileId，表示用户希望关联后通过 桶+文件ID 访问文件，
            // 这里将文件使用ID重命名，并将旧文件名使用文件external_data保存起来
            newInfo.put(FieldName.FIELD_CLFILE_NAME, fileRecord.get(FieldName.FIELD_CLFILE_ID));
            newInfo.put(
                    FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + "."
                            + FieldName.FIELD_CLFILE_FILE_EXT_NAME_BEFORE_ATTACH,
                    fileRecord.get(FieldName.FIELD_CLFILE_NAME));
        }

        return new BasicBSONObject("$set", newInfo);
    }

    private ScmBucketAttachFailure attachFile(ScmBucket attachToBucket, ScmWorkspaceInfo wsInfo,
            ContentModuleMetaSource metasource, String fileId, ScmBucketAttachKeyType attachKeyType,
            String username) {

        ScmLock lock = null;
        QuotaInfo quotaInfo = null;
        OperationCompleteCallback operationCompleteCallback = null;
        try {
            lock = ScmLockManager.getInstance().acquiresWriteLock(
                    ScmLockPathFactory.createFileLockPath(wsInfo.getName(), fileId));

            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), null);

            BasicBSONObject fileIdAndMonthMatcher = new BasicBSONObject();
            ScmMetaSourceHelper.addFileIdAndCreateMonth(fileIdAndMonthMatcher, fileId);
            BSONObject latestFileVersion = fileAccessor.queryOne(fileIdAndMonthMatcher, null, null);
            if (latestFileVersion == null) {
                ScmBucketAttachFailure failure = new ScmBucketAttachFailure(fileId,
                        ScmError.FILE_NOT_FOUND, "file not exist:ws=" + wsInfo.getName(), null);
                logger.warn("failed to attach file: bucket={}, failure={}", attachToBucket,
                        failure);
                return failure;
            }


            ScmBucket fileCurrentBucket = getFileBucket(latestFileVersion);
            if (fileCurrentBucket != null) {
                if (fileCurrentBucket.getName().equals(attachToBucket.getName())) {
                    return null;
                }
                ScmBucketAttachFailure failure = new ScmBucketAttachFailure(fileId,
                        ScmError.FILE_IN_ANOTHER_BUCKET,
                        "file already in another bucket:ws=" + wsInfo.getName() + ", bucketName="
                                + fileCurrentBucket.getName(),
                        new BasicBSONObject(CommonDefine.RestArg.BUCKET_NAME,
                                fileCurrentBucket.getName()));
                logger.warn("failed to attach file: bucket={}, failure={}", attachToBucket,
                        failure);
                return failure;
            }

            FileMeta latestVersionMeta = fileMetaFactory.createFileMetaByRecord(wsInfo.getName(),
                    latestFileVersion);

            List<FileMetaUpdater> fileMetaUpdaters = new ArrayList<>();
            fileMetaUpdaters.add(FileMetaDefaultUpdater.globalFieldUpdater(
                    FieldName.FIELD_CLFILE_FILE_BUCKET_ID, attachToBucket.getId()));
            if (attachKeyType == ScmBucketAttachKeyType.FILE_ID) {
                fileMetaUpdaters.add(FileMetaDefaultUpdater
                        .globalFieldUpdater(FieldName.FIELD_CLFILE_NAME, fileId));
                fileMetaUpdaters.add(FileMetaDefaultUpdater.globalFieldUpdater(
                        FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + "."
                                + FieldName.FIELD_CLFILE_FILE_EXT_NAME_BEFORE_ATTACH,
                        latestVersionMeta.getName()));
            }
            quotaInfo = quotaManager.acquireQuota(attachToBucket.getName(),
                    latestVersionMeta.getSize(), latestVersionMeta.getCreateTime());
            UpdateFileMetaResult ret = fileMetaOperator.updateFileMeta(wsInfo.getName(), fileId,
                    fileMetaUpdaters, username, new Date(), latestVersionMeta, null);
            operationCompleteCallback = listenerMgr.postUpdate(wsInfo, latestVersionMeta,
                    ret.getLatestVersionAfterUpdate());
            return null;
        }
        catch (Exception e) {
            quotaManager.releaseQuota(quotaInfo);
            ScmError scmError = getScmErrorFromException(e);
            ScmBucketAttachFailure failure = new ScmBucketAttachFailure(fileId, scmError,
                    e.getMessage(), null);
            logger.warn("failed to attach file: bucket={}, failure={}", attachToBucket, failure, e);
            return failure;
        }
        finally {
            if (lock != null) {
                lock.unlock();
            }
            if (operationCompleteCallback != null) {
                operationCompleteCallback.onComplete();
            }
        }
    }

    private ScmBucket getFileBucket(BSONObject file) throws ScmServerException {
        Number bucketId = BsonUtils.getNumber(file, FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
        if (bucketId != null) {
            return bucketInfoManager.getBucketById(bucketId.longValue());
        }
        return null;
    }

    private ScmError getScmErrorFromException(Exception e) {
        if (e instanceof ScmServerException) {
            return ((ScmServerException) e).getError();
        }
        if (e instanceof ScmMetasourceException) {
            return ((ScmMetasourceException) e).getScmError();
        }
        return ScmError.SYSTEM_ERROR;
    }

    @Override
    public FileMeta getFileVersion(ScmUser user, String bucketName, String fileName,
            int majorVersion, int minorVersion) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.READ, "get file in bucket");
        FileMeta ret = getFileVersion(bucketName, fileName, majorVersion, minorVersion);
        audit.info(ScmAuditType.FILE_DQL, user, bucket.getWorkspace(), 0,
                "get file in bucket: bucketName=" + bucketName + ", fileName=" + fileName
                        + ", version=" + majorVersion + "." + minorVersion);
        return ret;
    }

    @Override
    public FileMeta getFileVersion(String bucketName, String fileName, int majorVersion,
            int minorVersion) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        String fileId = getFileId(bucket, fileName);
        ScmContentModule contentModule = ScmContentModule.getInstance();
        ScmWorkspaceInfo ws = contentModule.getWorkspaceInfoCheckLocalSite(bucket.getWorkspace());
        BSONObject fileInfo = contentModule.getMetaService().getFileInfo(ws.getMetaLocation(),
                ws.getName(), fileId, majorVersion, minorVersion);
        if (fileInfo == null) {
            throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                    "file not exist: bucket=" + bucketName + ", fileName=" + fileName);
        }
        return fileMetaFactory.createFileMetaByRecord(ws.getName(), fileInfo);
    }

    @Override
    public FileMeta getFileNullVersion(ScmUser user, String bucketName, String fileName)
            throws ScmServerException {
        return getFileVersion(user, bucketName, fileName, CommonDefine.File.NULL_VERSION_MAJOR,
                CommonDefine.File.NULL_VERSION_MINOR);
    }

    @Override
    public void setBucketTag(ScmUser user, String bucketName, Map<String, String> customTag)
            throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.UPDATE, "set bucket tag");
        ScmArgChecker.Bucket.checkBucketTag(customTag);
        ContenserverConfClient.getInstance().updateBucketTag(user.getUsername(),
                bucketName, new TreeMap<>(customTag));
        audit.info(ScmAuditType.UPDATE_SCM_BUCKET, user, bucket.getWorkspace(), 0,
                "set bucket tag : bucketName=" + bucketName + ", ws=" + bucket.getWorkspace()
                        + ", tag=" + customTag);
    }

    @Override
    public Map<String, String> getBucketTag(ScmUser user, String bucketName)
            throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.READ, "get bucket tag");
        Map<String, String> customTag = bucket.getCustomTag();
        if (customTag == null || customTag.isEmpty()) {
            throw new ScmServerException(ScmError.BUCKET_CUSTOMTAG_NOT_EXIST,
                    "bucket tag does not exist : bucketName=" + bucketName);
        }
        return customTag;
    }

    @Override
    public void deleteBucketTag(ScmUser user, String bucketName) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.UPDATE, "delete bucket tag");
        Map<String, String> customTag = new HashMap<>();
        ContenserverConfClient.getInstance().updateBucketTag(user.getUsername(),
                bucketName, customTag);
        audit.info(ScmAuditType.UPDATE_SCM_BUCKET, user, bucket.getWorkspace(), 0,
                "delete bucket tag : bucketName=" + bucketName + ", ws=" + bucket.getWorkspace());
    }

    @Override
    public ObjectDeltaInfo getObjectDelta(String bucketName, BSONObject condition)
            throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckLocalSite(bucket.getWorkspace());
        BSONObject selector = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_SIZE, null);
        MetaCursor cursor = null;
        long count = 0;
        long size = 0;
        try {
            cursor = queryAllBucketFile(wsInfo, bucket, condition, selector, null, false);
            while (cursor.hasNext()) {
                BSONObject file = cursor.getNext();
                count++;
                size += BsonUtils.getNumber(file, FieldName.FIELD_CLFILE_FILE_SIZE).longValue();
            }
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.SYSTEM_ERROR, "failed to calculate object delta",
                    e);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return new ObjectDeltaInfo(bucketName, count, size);
    }

    private void transactionRollback(TransactionContext transactionContext) {
        if (transactionContext != null) {
            transactionContext.rollback();
        }
    }

    private void transactionClose(TransactionContext transactionContext) {
        if (transactionContext != null) {
            transactionContext.close();
        }
    }
}
