package com.sequoiacm.contentserver.service.impl;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;
import com.sequoiacm.common.module.ScmBucketAttachFailure;
import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.bizconfig.ContenserverConfClient;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.common.AsyncUtils;
import com.sequoiacm.contentserver.common.ScmFileOperateUtils;
import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.contentserver.dao.FileAddVersionDao;
import com.sequoiacm.contentserver.dao.FileDeletorDao;
import com.sequoiacm.contentserver.dao.FileInfoAndOpCompleteCallback;
import com.sequoiacm.contentserver.dao.FileVersionDeleteDao;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.listener.FileOperationListenerMgr;
import com.sequoiacm.contentserver.listener.OperationCompleteCallback;
import com.sequoiacm.contentserver.lock.ScmLockManager;
import com.sequoiacm.contentserver.lock.ScmLockPath;
import com.sequoiacm.contentserver.lock.ScmLockPathFactory;
import com.sequoiacm.contentserver.metasourcemgr.ScmMetaSourceHelper;
import com.sequoiacm.contentserver.model.BreakpointFile;
import com.sequoiacm.contentserver.model.SessionInfoWrapper;
import com.sequoiacm.contentserver.model.OverwriteOption;
import com.sequoiacm.contentserver.model.ScmBucket;
import com.sequoiacm.contentserver.model.ScmDataInfoDetail;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.IDatasourceService;
import com.sequoiacm.contentserver.service.IScmBucketService;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.datasource.dataoperation.ScmDataInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.audit.ScmAudit;
import com.sequoiacm.infrastructure.audit.ScmAuditType;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.common.ScmIdGenerator;
import com.sequoiacm.infrastructure.common.ScmObjectCursor;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.metasource.ContentModuleMetaSource;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaCursor;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.sequoiadb.SequoiadbHelper;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    @Override
    public ScmBucket createBucket(ScmUser user, String ws, String name) throws ScmServerException {
        ScmFileServicePriv.getInstance().checkWsPriority(user, ws, ScmPrivilegeDefine.CREATE,
                "create bucket");
        ScmWorkspaceInfo workspaceInfo = ScmContentModule.getInstance().getWorkspaceInfo(ws);
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
    public long countFile(ScmUser user, String bucketName, BSONObject condition)
            throws ScmServerException {
        ScmBucket bucket = bucketInfoManager.getBucket(bucketName);
        if (bucket == null) {
            throw new ScmServerException(ScmError.BUCKET_NOT_EXISTS,
                    "bucket not exist:" + bucketName);
        }
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.READ, "count bucket file");

        long ret = countFile(bucket, condition);
        audit.info(ScmAuditType.FILE_DQL, user, bucket.getWorkspace(), 0,
                "count file in bucket: bucketName=" + bucketName + ", ws=" + bucket.getWorkspace()
                        + ", condition=" + condition);
        return ret;
    }

    public long countFile(ScmBucket bucket, BSONObject condition) throws ScmServerException {
        try {
            MetaAccessor accessor = bucket.getFileTableAccessor(null);
            return accessor.count(condition);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(), "failed to count bucket file: bucket="
                    + bucket.getName() + ", condition=" + condition, e);
        }
    }

    @Override
    public ScmBucket getBucket(ScmUser user, String name) throws ScmServerException {
        ScmBucket bucket = getBucket(name);
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
    public ScmBucket deleteBucket(ScmUser user, String name) throws ScmServerException {
        ScmBucket bucket = getBucket(name);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.DELETE, "delete bucket");

        if (countFile(bucket, null) > 0) {
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
        return ContenserverConfClient.getInstance().listBucket(condition,
                orderBy, skip, limit);
    }

    @Override
    public long countBucket(ScmUser user, BSONObject condition) throws ScmServerException {
        return ContenserverConfClient.getInstance().countBucket(condition);
    }

    @Override
    public BSONObject createFile(ScmUser user, String bucketName, BSONObject fileInfo,
            InputStream data, OverwriteOption overwriteOption) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.CREATE, "create file in bucket");
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfo(bucket.getWorkspace());
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

        BSONObject createdFile;
        Number fileCreateTime = BsonUtils.getNumber(fileInfo,
                FieldName.FIELD_CLFILE_INNER_CREATE_TIME);
        if (fileCreateTime == null) {
            fileCreateTime = System.currentTimeMillis();
            fileInfo.put(FieldName.FIELD_CLFILE_INNER_CREATE_TIME, fileCreateTime);
        }
        ScmDataInfoDetail dataDetail = datasourceService.createData(wsInfo.getName(), data,
                fileCreateTime.longValue());
        try {
            createdFile = createFile(user, bucketName, fileInfo, dataDetail, null, overwriteOption);
        }
        catch (ScmServerException e) {
            if (e.getError() != ScmError.COMMIT_UNCERTAIN_STATE) {
                deleteDataSilence(wsInfo.getName(), dataDetail);
            }
            throw e;
        }
        catch (Exception e) {
            deleteDataSilence(wsInfo.getName(), dataDetail);
            throw e;
        }
        return createdFile;
    }

    private void deleteDataSilence(String ws, ScmDataInfoDetail dataDetail) {
        try {
            datasourceService.deleteData(ws, dataDetail.getDataInfo().getId(),
                    dataDetail.getDataInfo().getType(),
                    dataDetail.getDataInfo().getCreateTime().getTime(), dataDetail.getSiteId());
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
    public BSONObject createFile(ScmUser user, String bucketName, BSONObject fileInfo,
            final String breakpointFileName, OverwriteOption overwriteOption)
            throws ScmServerException {
        final ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.CREATE, "create file in bucket");

        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfo(bucket.getWorkspace());
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

        ScmLock lock = lockBreakpointFile(bucket.getWorkspace(), breakpointFileName);
        FileInfoAndOpCompleteCallback fileInfoAndOpCompleteCallback;
        try {
            BreakpointFile breakpointFile = ScmContentModule.getInstance().getMetaService()
                    .getBreakpointFile(bucket.getWorkspace(), breakpointFileName);
            if (breakpointFile == null) {
                throw new ScmInvalidArgumentException(
                        String.format("BreakpointFile is not found: /%s/%s", bucket.getWorkspace(),
                                breakpointFileName));
            }
            if (!breakpointFile.isCompleted()) {
                throw new ScmInvalidArgumentException(
                        String.format("Uncompleted BreakpointFile: /%s/%s", bucket.getWorkspace(),
                                breakpointFileName));
            }
            if (!breakpointFile.isNeedMd5()) {
                throw new ScmInvalidArgumentException(
                        String.format("BreakpointFile has no md5: /%s/%s", bucket.getWorkspace(),
                                breakpointFileName));
            }

            ScmDataInfoDetail dataDetail = new ScmDataInfoDetail(
                    new ScmDataInfo(ENDataType.Normal.getValue(), breakpointFile.getDataId(),
                            new Date(breakpointFile.getCreateTime())));
            dataDetail.setMd5(breakpointFile.getMd5());
            dataDetail.setSize(breakpointFile.getUploadSize());
            dataDetail.setSiteId(breakpointFile.getSiteId());

            fileInfoAndOpCompleteCallback = createFileMeta(user, fileInfo, dataDetail,
                    new TransactionCallback() {
                        @Override
                        public void beforeTransactionCommit(TransactionContext context)
                                throws ScmServerException {
                            ScmContentModule.getInstance().getMetaService().deleteBreakpointFile(
                                    bucket.getWorkspace(), breakpointFileName, context);
                        }
                    }, overwriteOption, bucket, wsInfo);
        }
        finally {
            lock.unlock();
        }
        BSONObject createdFile = fileInfoAndOpCompleteCallback.getFileInfo();
        audit.info(ScmAuditType.CREATE_FILE, user, wsInfo.getName(), 0,
                "create file in bucket: bucketName=" + bucketName + ", fileId="
                        + createdFile.get(FieldName.FIELD_CLFILE_ID) + ", fileName="
                        + fileInfo.get(FieldName.FIELD_CLFILE_NAME));
        fileInfoAndOpCompleteCallback.getCallback().onComplete();
        return createdFile;
    }

    @Override
    public BSONObject createFile(ScmUser user, String bucketName, BSONObject fileInfo,
            ScmDataInfoDetail data, TransactionCallback transactionCallback,
            OverwriteOption overwriteOption) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.CREATE, "create file in bucket");

        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfo(bucket.getWorkspace());
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

        FileInfoAndOpCompleteCallback ret = createFileMeta(user, fileInfo, data,
                transactionCallback, overwriteOption, bucket, wsInfo);
        BSONObject createdFile = ret.getFileInfo();
        audit.info(ScmAuditType.CREATE_FILE, user, wsInfo.getName(), 0,
                "create file in bucket: bucketName=" + bucketName + ", fileId="
                        + createdFile.get(FieldName.FIELD_CLFILE_ID) + ", fileName="
                        + fileInfo.get(FieldName.FIELD_CLFILE_NAME));
        ret.getCallback().onComplete();
        return createdFile;
    }

    private FileInfoAndOpCompleteCallback createFileMeta(ScmUser user, BSONObject fileInfo,
            ScmDataInfoDetail data, TransactionCallback transactionCallback,
            OverwriteOption overwriteOption, ScmBucket bucket, ScmWorkspaceInfo wsInfo)
            throws ScmServerException {
        long fileCreateTime = BsonUtils.getNumberOrElse(fileInfo,
                FieldName.FIELD_CLFILE_INNER_CREATE_TIME, System.currentTimeMillis()).longValue();
        Date fileCreateDate = new Date(fileCreateTime);
        String fileId = ScmIdGenerator.FileId.get(fileCreateDate);

        BSONObject checkedFileObj = ScmFileOperateUtils.formatFileObj(wsInfo, fileInfo, fileId,
                fileCreateDate, user.getUsername());
        ScmFileOperateUtils.addDataInfo(checkedFileObj, data.getDataInfo().getId(),
                data.getDataInfo().getCreateTime(), data.getSiteId(), data.getSize(),
                data.getMd5());
        checkedFileObj.put(FieldName.FIELD_CLFILE_FILE_BUCKET_ID, bucket.getId());

        return insertMeta(bucket, wsInfo, checkedFileObj,
                transactionCallback, overwriteOption);
    }

    private FileInfoAndOpCompleteCallback insertMeta(ScmBucket bucket, ScmWorkspaceInfo wsInfo,
            BSONObject checkedFileObj,
            TransactionCallback transactionCallback, OverwriteOption overwriteOption)
            throws ScmServerException {
        BSONObject originalFileObj = new BasicBSONObject();
        originalFileObj.putAll(checkedFileObj);
        listenerMgr.preCreate(wsInfo, checkedFileObj);
        if (bucket.getVersionStatus() != ScmBucketVersionStatus.Enabled) {
            checkedFileObj.put(FieldName.FIELD_CLFILE_NULL_MARKER, true);
        }
        BSONObject bucketFileRel = ScmFileOperateUtils.createBucketFileRel(checkedFileObj);
        ContentModuleMetaSource metaSource = ScmContentModule.getInstance().getMetaService()
                .getMetaSource();
        MetaFileAccessor fileAccessor = null;
        TransactionContext trans = null;
        try {
            trans = metaSource.createTransactionContext();
            MetaAccessor bucketFileTable = bucket.getFileTableAccessor(trans);
            fileAccessor = metaSource.getFileAccessor(wsInfo.getMetaLocation(), wsInfo.getName(),
                    trans);
            trans.begin();

            try {
                bucketFileTable.insert(bucketFileRel);
            }
            catch (ScmMetasourceException e) {
                transactionRollback(trans);
                if (e.getScmError() == ScmError.METASOURCE_RECORD_EXIST) {
                    if (overwriteOption.isOverwrite()) {
                        overwriteFile(wsInfo, bucket, bucketFileRel, checkedFileObj,
                                transactionCallback, overwriteOption);
                        OperationCompleteCallback operationCompleteCallback = listenerMgr
                                .postCreate(wsInfo, BsonUtils.getStringChecked(checkedFileObj,
                                        FieldName.FIELD_CLFILE_ID));
                        return new FileInfoAndOpCompleteCallback(checkedFileObj,
                                operationCompleteCallback);
                    }
                    try {
                        return createNewFileVersion(wsInfo, bucket, checkedFileObj,
                                transactionCallback);
                    }
                    catch (ScmServerException ex) {
                        if (ex.getError() == ScmError.FILE_NOT_FOUND) {
                            // 创建文件时发现桶下已存在同名文件，这里尝试给已存在的文件新增版本，又发生了文件不存在的错误，不再重试，报错
                            throw new ScmServerException(ScmError.RESOUCE_CONFLICT,
                                    "failed to create new version for file, file is busy: ws="
                                            + wsInfo.getName() + ", bucket=" + bucket.getName()
                                            + ", fileName="
                                            + checkedFileObj.get(FieldName.FIELD_CLFILE_NAME),
                                    e);
                        }
                        throw ex;
                    }
                }
                throw e;
            }

            fileAccessor.insert(checkedFileObj);

            invokeTransactionCallback(transactionCallback, trans);
            trans.commit();
            OperationCompleteCallback operationCompleteCallback = listenerMgr.postCreate(wsInfo,
                    BsonUtils.getStringChecked(checkedFileObj, FieldName.FIELD_CLFILE_ID));
            return new FileInfoAndOpCompleteCallback(checkedFileObj, operationCompleteCallback);
        }
        catch (ScmMetasourceException e) {
            // 可能重复做了 transRollback，但 trans 对象底下会忽略，所以没影响
            transactionRollback(trans);
            if (e.getScmError() == ScmError.FILE_TABLE_NOT_FOUND) {
                try {
                    fileAccessor.createFileTable(checkedFileObj);
                }
                catch (ScmMetasourceException ex) {
                    throw new ScmServerException(ScmError.METASOURCE_ERROR,
                            "insert file failed, create file table failed:ws=" + wsInfo.getName()
                                    + ",file=" + checkedFileObj,
                            ex);
                }
                return insertMeta(bucket, wsInfo, originalFileObj, transactionCallback,
                        overwriteOption);
            }
            throw new ScmServerException(e.getScmError(), "failed to create file in bucket: bucket="
                    + bucket.getName() + ", file=" + checkedFileObj, e);
        }
        catch (Exception e) {
            transactionRollback(trans);
            throw e;
        }
        finally {
            transactionClose(trans);
        }
    }

    private FileInfoAndOpCompleteCallback createNewFileVersion(ScmWorkspaceInfo ws,
            ScmBucket bucket, BSONObject newFileVersion, TransactionCallback transactionCallback)
            throws ScmServerException {
        String fileId = getFileId(bucket,
                BsonUtils.getStringChecked(newFileVersion, FieldName.FIELD_CLFILE_NAME));
        FileAddVersionDao dao = new FileAddVersionDao(ws, fileId, transactionCallback,
                bucketInfoManager, listenerMgr);
        return dao.addVersion(FileAddVersionDao.Context.standard(newFileVersion));
    }

    private void overwriteFile(final ScmWorkspaceInfo wsInfo, final ScmBucket bucket,
            BSONObject bucketFileRel, BSONObject checkedFileObj,
            TransactionCallback transactionCallback, final OverwriteOption overwriteOption)
            throws ScmServerException {
        ContentModuleMetaSource metaSource = ScmContentModule.getInstance().getMetaService()
                .getMetaSource();
        TransactionContext trans = null;
        try {
            trans = metaSource.createTransactionContext();
            MetaAccessor bucketFileTable = bucket.getFileTableAccessor(trans);
            MetaFileAccessor fileAccessor = metaSource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), trans);
            trans.begin();
            BSONObject oldBucketRel = bucketFileTable.queryAndDelete(new BasicBSONObject(
                    FieldName.BucketFile.FILE_NAME,
                    BsonUtils.getStringChecked(checkedFileObj, FieldName.FIELD_CLFILE_NAME)));
            try {
                bucketFileTable.insert(bucketFileRel);
            }
            catch (ScmMetasourceException e) {
                if (e.getScmError() == ScmError.METASOURCE_RECORD_EXIST) {
                    // 再次插入桶文件关系记录发生索引冲突的场景：
                    // 桶中已存在文件： file1，存在3个线程时序如下：
                    //
                    // t1---创建file1（索引冲突）-----------------queryAndDelete-------------------------再次创建file1（索引冲突）
                    // t2---------------------------删除file1
                    // t3------------------------------------------------------------创建file1
                    //
                    // t1 的最后一步异常处理（即本逻辑），递归再尝试
                    trans.rollback();
                    trans.close();
                    trans = null;
                    logger.warn("try overwrite file again: bucket={}, fileName={}",
                            bucket.getName(), checkedFileObj.get(FieldName.FIELD_CLFILE_NAME), e);
                    overwriteFile(wsInfo, bucket, bucketFileRel, checkedFileObj,
                            transactionCallback, overwriteOption);
                    return;
                }
                throw e;
            }
            fileAccessor.insert(checkedFileObj);

            if (oldBucketRel == null) {
                invokeTransactionCallback(transactionCallback, trans);
                trans.commit();
                return;
            }

            // 更新被覆盖文件记录，bucket_id 置空
            // 这里没有删除旧文件，旧文件的删除在另外一个事务处理，防止死锁
            // 如果在这里删除文件需要获取文件写锁，而锁外我们已经事务修改了桶文件关系表记录；这个时序会与删除文件产生死锁（获取文件锁，事务删除桶文件关系表）
            BSONObject fileMatcher = new BasicBSONObject();
            ScmMetaSourceHelper.addFileIdAndCreateMonth(fileMatcher,
                    BsonUtils.getStringChecked(oldBucketRel, FieldName.BucketFile.FILE_ID));
            BasicBSONObject updater = new BasicBSONObject(
                    ScmMetaSourceHelper.SEQUOIADB_MODIFIER_SET,
                    new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_BUCKET_ID, null));
            final BSONObject oldFile = fileAccessor.queryAndUpdate(fileMatcher, updater, null);

            invokeTransactionCallback(transactionCallback, trans);
            trans.commit();

            // 异步删除旧文件
            if (oldFile != null) {
                AsyncUtils.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            removeFile(wsInfo, oldFile,
                                    overwriteOption.getSessionInfoWrapper().getSessionId(),
                                    overwriteOption.getSessionInfoWrapper().getUserDetail());
                        }
                        catch (Exception e) {
                            logger.warn(
                                    "create file in bucket success, but failed to remove the old file: bucket={}, oldFile={}",
                                    bucket.getName(), oldFile, e);
                        }
                    }
                });
            }
        }
        catch (ScmServerException e) {
            transactionRollback(trans);
            throw e;
        }
        catch (Exception e) {
            transactionRollback(trans);
            throw new ScmServerException(ScmError.METASOURCE_ERROR,
                    "failed to overwrite file: bucket=" + bucket.getName() + ", fileName="
                            + checkedFileObj.get(FieldName.FIELD_CLFILE_NAME),
                    e);
        }
        finally {
            transactionClose(trans);
        }
    }

    private void invokeTransactionCallback(TransactionCallback c, TransactionContext context)
            throws ScmServerException, ScmMetasourceException {
        if (c != null) {
            c.beforeTransactionCommit(context);
        }
    }

    private void removeFile(ScmWorkspaceInfo wsInfo, BSONObject file, String sessionId,
            String userDetail) throws ScmServerException {
        FileDeletorDao dao = new FileDeletorDao(sessionId, userDetail, listenerMgr,
                bucketInfoManager);
        dao.init(wsInfo, BsonUtils.getStringChecked(file, FieldName.FIELD_CLFILE_ID), true);
        dao.delete();
    }

    @Override
    public MetaCursor listFile(ScmUser user, String bucketName, BSONObject condition, BSONObject selector,
            BSONObject orderBy, long skip, long limit)
            throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.READ, "list file in bucket");
        MetaCursor ret = listFile(condition, selector, orderBy, skip, limit, bucket);
        audit.info(ScmAuditType.FILE_DQL, user, bucket.getWorkspace(), 0,
                "list file in bucket: bucketName=" + bucketName + ", condition=" + condition
                        + ", orderby=" + orderBy + ", skip=" + skip + ", limit=" + limit);
        return ret;
    }

    private MetaCursor listFile(BSONObject condition, BSONObject selector,
            BSONObject orderBy, long skip, long limit, ScmBucket bucket) throws ScmServerException {
        try {
            MetaAccessor accessor = bucket.getFileTableAccessor(null);
            return accessor.query(condition, selector, orderBy, skip, limit, 0);
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to list file, bucket not exist:bucket=" + bucket.getName()
                            + ", condition=" + condition + ", orderby=" + orderBy + ", skip=" + skip
                            + ", limit=" + limit,
                    e);
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
                "get file in bucket: bucketName=" + bucketName + ", fileName=" + fileName + ", fileId="
                        + fileId);
        return fileId;
    }

    @Override
    public List<ScmBucketAttachFailure> attachFile(ScmUser user, String bucketName,
            List<String> fileIdList, ScmBucketAttachKeyType type) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.CREATE, "bucket attach file");
        ScmWorkspaceInfo wsInfo = ScmContentModule.getInstance()
                .getWorkspaceInfo(bucket.getWorkspace());
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
            ScmBucketAttachFailure failure = attachFile(bucket, wsInfo, metasource, fileId, type);
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
                .getWorkspaceInfo(bucket.getWorkspace());
        if (wsInfo == null) {
            throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST,
                    "bucket workspace not exist: bucket=" + bucketName + ",workspace="
                            + bucket.getWorkspace());
        }
        ContentModuleMetaSource metasource = ScmContentModule.getInstance().getMetaService()
                .getMetaSource();

        String fileId = getFileId(user, bucketName, fileName);

        ScmLock lock = ScmLockManager.getInstance()
                .acquiresWriteLock(ScmLockPathFactory.createFileLockPath(wsInfo.getName(), fileId));
        OperationCompleteCallback operationCompleteCallback = null;
        TransactionContext trans = null;
        try {
            trans = metasource.createTransactionContext();
            trans.begin();
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), trans);
            MetaAccessor bucketFileAccessor = bucket.getFileTableAccessor(trans);
            BasicBSONObject bucketFileMatcher = new BasicBSONObject();
            bucketFileMatcher.put(FieldName.BucketFile.FILE_NAME, fileName);
            BSONObject deletedBucketFile = bucketFileAccessor.queryAndDelete(bucketFileMatcher);
            if (deletedBucketFile == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "bucket file not found: bucket=" + bucketName + ", fileName=" + fileName);
            }
            if (!fileId.equals(deletedBucketFile.get(FieldName.BucketFile.FILE_ID))) {
                // 锁内查出来的关系表记录，文件id和锁外不一样，直接返回，有其它多个线程在并发向这个bucket下创建同名文件、删除同名文件
                // 客户端可以认为服务端已经做过一次解除关联，但是被其它线程重新在这个Bucket下又建了一个同名文件
                trans.rollback();
                audit.info(ScmAuditType.UPDATE_FILE, user, bucket.getWorkspace(), 0,
                        "bucket detach file (file already detach): bucketName=" + bucketName
                                + ", fileName=" + fileName + ", fileId=" + fileId);
                return;
            }
            BSONObject fileMatcher = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_BUCKET_ID,
                    bucket.getId());
            SequoiadbHelper.addFileIdAndCreateMonth(bucketFileMatcher, fileId);
            BSONObject bucketIdSetNull = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_BUCKET_ID,
                    null);
            BSONObject updater = new BasicBSONObject("$set", bucketIdSetNull);
            BSONObject newFileInfo = fileAccessor.queryAndUpdate(fileMatcher, updater, null, true);
            if (newFileInfo != null) {
                operationCompleteCallback = listenerMgr.postUpdate(wsInfo, newFileInfo);
            }
            trans.commit();
            audit.info(ScmAuditType.UPDATE_FILE, user, bucket.getWorkspace(), 0,
                    "bucket detach file: bucketName=" + bucketName + ", fileName=" + fileName
                            + ", fileId=" + fileId);
        }
        catch (ScmMetasourceException e) {
            transactionRollback(trans);
            throw new ScmServerException(ScmError.METASOURCE_ERROR,
                    "failed to detach bucket file: bucket=" + bucketName + ", fileName=" + fileName,
                    e);
        }
        catch (Exception e) {
            transactionRollback(trans);
            throw e;
        }
        finally {
            transactionClose(trans);
            lock.unlock();
            if (operationCompleteCallback != null) {
                operationCompleteCallback.onComplete();
            }
        }
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
                "update bucket version status: bucketName=" + bucketName + ", ws=" + bucket.getWorkspace()
                        + ", versionStatus=" + bucketVersionStatus);
        return ret;
    }

    @Override
    public BSONObject deleteFile(ScmUser user, String bucketName, String fileName,
            boolean isPhysical, SessionInfoWrapper sessionInfoWrapper) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.DELETE, "delete bucket file");
        FileDeletorDao dao = new FileDeletorDao(sessionInfoWrapper.getSessionId(),
                sessionInfoWrapper.getUserDetail(), listenerMgr, bucketInfoManager);
        dao.init(user.getUsername(), bucket, fileName, isPhysical);
        BSONObject ret = dao.delete();
        audit.info(ScmAuditType.DELETE_FILE, user, bucket.getWorkspace(), 0,
                "delete file in bucket: bucketName=" + bucketName + ", fileName=" + fileName
                        + ", isPhysical=" + isPhysical);
        return ret;
    }

    @Override
    public BSONObject deleteFileVersion(ScmUser user, String bucketName, String fileName,
            int majorVersion, int minorVersion) throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.DELETE, "delete bucket file version");
        FileVersionDeleteDao fileVersionDeleter = new FileVersionDeleteDao(bucket, fileName,
                majorVersion, minorVersion, listenerMgr, bucketInfoManager);
        BSONObject ret = fileVersionDeleter.delete();
        audit.info(ScmAuditType.DELETE_FILE, user, bucket.getWorkspace(), 0,
                "delete file version in bucket: bucketName=" + bucketName + ", fileName=" + fileName
                        + ", version=" + majorVersion + "." + minorVersion);
        return ret;
    }

    @Override
    public BSONObject deleteFileVersionNullMarker(ScmUser user, String bucketName, String fileName)
            throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.DELETE, "delete bucket file version");
        BSONObject fileVersion = getFileNullMarkerVersion(user, bucketName, fileName);
        FileVersionDeleteDao fileVersionDeleter = new FileVersionDeleteDao(bucket, fileName,
                BsonUtils.getIntegerChecked(fileVersion, FieldName.FIELD_CLFILE_MAJOR_VERSION),
                BsonUtils.getIntegerChecked(fileVersion, FieldName.FIELD_CLFILE_MINOR_VERSION),
                listenerMgr, bucketInfoManager);
        BSONObject ret = fileVersionDeleter.delete();
        audit.info(ScmAuditType.DELETE_FILE, user, bucket.getWorkspace(), 0,
                "delete file version in bucket: bucketName=" + bucketName + ", fileName=" + fileName
                        + ", version=nullMarker");
        return ret;
    }

    private ScmBucketAttachFailure attachFile(ScmBucket bucket, ScmWorkspaceInfo wsInfo,
            ContentModuleMetaSource metasource, String fileId, ScmBucketAttachKeyType type) {
        BSONObject newInfo = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_BUCKET_ID,
                bucket.getId());
        if (bucket.getVersionStatus() != ScmBucketVersionStatus.Enabled) {
            newInfo.put(FieldName.FIELD_CLFILE_NULL_MARKER, true);
        }
        BasicBSONObject updater = new BasicBSONObject("$set", newInfo);

        ScmLock lock = null;
        TransactionContext trans = null;
        OperationCompleteCallback operationCompleteCallback = null;
        try {
            lock = ScmLockManager.getInstance().acquiresWriteLock(
                    ScmLockPathFactory.createFileLockPath(wsInfo.getName(), fileId));
            trans = metasource.createTransactionContext();
            trans.begin();
            MetaFileAccessor fileAccessor = metasource.getFileAccessor(wsInfo.getMetaLocation(),
                    wsInfo.getName(), trans);
            MetaAccessor bucketFileAccessor = bucket.getFileTableAccessor(trans);

            BasicBSONObject matcher = new BasicBSONObject();
            ScmMetaSourceHelper.addFileIdAndCreateMonth(matcher, fileId);

            BSONObject oldFileRecord = fileAccessor.queryAndUpdate(matcher, updater, null);
            if (oldFileRecord == null) {
                ScmBucketAttachFailure failure = new ScmBucketAttachFailure(fileId,
                        ScmError.FILE_NOT_FOUND, "file not exist:ws=" + wsInfo.getName(), null);
                logger.warn("failed to attach file: bucket={}, failure={}", bucket, failure);
                transactionRollback(trans);
                return failure;
            }
            Number oldBucketId = BsonUtils.getNumber(oldFileRecord,
                    FieldName.FIELD_CLFILE_FILE_BUCKET_ID);
            if (oldBucketId != null) {
                if (oldBucketId.longValue() != bucket.getId()) {
                    ScmBucket oldBucket = bucketInfoManager.getBucketById(oldBucketId.longValue());
                    if (oldBucket != null) {
                        ScmBucketAttachFailure failure = new ScmBucketAttachFailure(fileId,
                                ScmError.FILE_IN_ANOTHER_BUCKET,
                                "file already in another bucket:ws=" + wsInfo.getName()
                                        + ", bucketName=" + oldBucket.getName(),
                                new BasicBSONObject(CommonDefine.RestArg.BUCKET_NAME,
                                        oldBucket.getName()));
                        transactionRollback(trans);
                        logger.warn("failed to attach file: bucket={}, failure={}", bucket,
                                failure);
                        return failure;
                    }
                }
                else {
                    transactionRollback(trans);
                    return null;
                }
            }
            if (bucket.getVersionStatus() == ScmBucketVersionStatus.Disabled) {
                if (!isFirstVersion(oldFileRecord)) {
                    ScmBucketAttachFailure failure = new ScmBucketAttachFailure(fileId,
                            ScmError.OPERATION_UNSUPPORTED,
                            "file has multi version, can not attach to the bucket(version control is disabled):ws="
                                    + wsInfo.getName() + ", bucketName=" + bucket.getName()
                                    + ", versionStatus=" + bucket.getVersionStatus(),
                            null);
                    transactionRollback(trans);
                    logger.warn("failed to attach file: bucket={}, failure={}", bucket, failure);
                    return failure;
                }
            }
            BSONObject newFileInfo = oldFileRecord;
            newFileInfo.put(FieldName.FIELD_CLFILE_FILE_BUCKET_ID, bucket.getId());
            if (type == ScmBucketAttachKeyType.FILE_ID) {
                // 关联类型是fileId，表示用户希望关联后通过 桶+文件ID 访问文件，
                // 这里将文件使用ID重命名，并将旧文件名使用文件external_data保存起来
                BasicBSONObject renameUpdator = new BasicBSONObject();
                renameUpdator.put(FieldName.FIELD_CLFILE_NAME, fileId);
                renameUpdator.put(
                        FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + "."
                                + FieldName.FIELD_CLFILE_FILE_EXT_NAME_BEFORE_ATTACH,
                        oldFileRecord.get(FieldName.FIELD_CLFILE_NAME));
                fileAccessor.update(matcher, new BasicBSONObject("$set", renameUpdator));
                newFileInfo.put(FieldName.FIELD_CLFILE_NAME, fileId);
            }

            // newFileInfo external_data字段是旧的，但是createBucketFileRel函数不关心这个字段
            BSONObject bucketFileRel = ScmFileOperateUtils.createBucketFileRel(newFileInfo);
            try {
                bucketFileAccessor.insert(bucketFileRel);
            }
            catch (ScmMetasourceException e) {
                if (e.getScmError() == ScmError.METASOURCE_RECORD_EXIST) {
                    throw new ScmServerException(ScmError.FILE_EXIST,
                            "the bucket already contain a file with same name: bucket="
                                    + bucket.getName() + ", ws=" + wsInfo.getName() + ", fileName="
                                    + bucketFileRel.get(FieldName.BucketFile.FILE_NAME),
                            e);
                }
                throw e;
            }
            trans.commit();
            operationCompleteCallback = listenerMgr.postUpdate(wsInfo, newFileInfo);
            return null;
        }
        catch (Exception e) {
            transactionRollback(trans);
            ScmError scmError = getScmErrorFromException(e);
            ScmBucketAttachFailure failure = new ScmBucketAttachFailure(fileId, scmError,
                    e.getMessage(), null);
            logger.warn("failed to attach file: bucket={}, failure={}", bucket, failure, e);
            return failure;
        }
        finally {
            transactionClose(trans);
            if (lock != null) {
                lock.unlock();
            }
            if (operationCompleteCallback != null) {
                operationCompleteCallback.onComplete();
            }
        }
    }

    private boolean isFirstVersion(BSONObject file) {
        int majorVersion = BsonUtils.getIntegerChecked(file, FieldName.FIELD_CLFILE_MAJOR_VERSION);
        int minorVersion = BsonUtils.getIntegerChecked(file, FieldName.FIELD_CLFILE_MINOR_VERSION);
        return majorVersion == 1 && minorVersion == 0;
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
    public BSONObject getFileVersion(ScmUser user, String bucketName, String fileName,
            int majorVersion, int minorVersion)
            throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.READ, "get file in bucket");
        BSONObject ret = getFileVersion(bucketName, fileName, majorVersion, minorVersion);
        audit.info(ScmAuditType.FILE_DQL, user, bucket.getWorkspace(), 0,
                "get file in bucket: bucketName=" + bucketName + ", fileName=" + fileName + ", version="
                        + majorVersion + "." + minorVersion);
        return ret;
    }

    public BSONObject getFileVersion(String bucketName, String fileName, int majorVersion,
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
        return fileInfo;

    }

    @Override
    public BSONObject getFileNullMarkerVersion(String bucketName, String fileName)
            throws ScmServerException {
        BSONObject file = getFileVersion(bucketName, fileName, -1, -1);
        if (file == null) {
            throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                    "file not exist: bucket=" + bucketName + ", fileName=" + fileName);
        }
        if (BsonUtils.getBooleanOrElse(file, FieldName.FIELD_CLFILE_NULL_MARKER, false)) {
            return file;
        }

        ScmBucket bucket = getBucket(bucketName);
        ScmWorkspaceInfo ws = ScmContentModule.getInstance()
                .getWorkspaceInfoCheckExist(bucket.getWorkspace());
        BasicBSONObject fileIdMatcher = new BasicBSONObject();
        ScmMetaSourceHelper.addFileIdAndCreateMonth(fileIdMatcher,
                BsonUtils.getStringChecked(file, FieldName.FIELD_CLFILE_ID));
        BSONObject nullMarkerIsTrue = new BasicBSONObject(FieldName.FIELD_CLFILE_NULL_MARKER, true);
        BasicBSONList andArr = new BasicBSONList();
        andArr.add(fileIdMatcher);
        andArr.add(nullMarkerIsTrue);
        BSONObject matcher = new BasicBSONObject("$and", andArr);
        try {
            MetaFileHistoryAccessor fileHistoryAccessor = ScmContentModule.getInstance()
                    .getMetaService().getMetaSource()
                    .getFileHistoryAccessor(ws.getMetaLocation(), ws.getName(), null);
            BSONObject historyFile = fileHistoryAccessor.queryOne(matcher, null);
            if (historyFile == null) {
                throw new ScmServerException(ScmError.FILE_NOT_FOUND,
                        "specify version not exist: bucket=" + bucketName + ", fileName="
                                + fileName);
            }
            return historyFile;
        }
        catch (ScmMetasourceException e) {
            throw new ScmServerException(e.getScmError(),
                    "failed to query file in history table: ws=" + ws.getName() + ", matcher="
                            + matcher,
                    e);
        }
    }

    @Override
    public BSONObject getFileNullMarkerVersion(ScmUser user, String bucketName, String fileName)
            throws ScmServerException {
        ScmBucket bucket = getBucket(bucketName);
        ScmFileServicePriv.getInstance().checkBucketPriority(user, bucket.getWorkspace(),
                bucket.getName(), ScmPrivilegeDefine.READ, "get file in bucket");
        BSONObject ret = getFileNullMarkerVersion(bucketName, fileName);
        audit.info(ScmAuditType.FILE_DQL, user, bucket.getWorkspace(), 0,
                "get null marker file in bucket: bucketName=" + bucketName + ", fileName=" + fileName);
        return ret;
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

