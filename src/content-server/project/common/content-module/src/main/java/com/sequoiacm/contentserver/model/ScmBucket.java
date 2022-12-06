package com.sequoiacm.contentserver.model;

import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.contentserver.bucket.BucketInfoManager;
import com.sequoiacm.contentserver.site.ScmContentModule;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.*;
import org.bson.BSONObject;

import java.util.List;
import java.util.Map;

public class ScmBucket {
    private final BucketInfoManager bucketInfoMgr;
    private String name;
    private long id;
    private long createTime;
    private String createUser;
    private String workspace;
    private String fileTable;
    private ScmBucketVersionStatus versionStatus;
    private Map<String, String> customTag;
    private String updateUser;
    private long updateTime;

    public ScmBucket(String name, long id, long createTime, String createUser, String workspace,
            String fileTable, ScmBucketVersionStatus versionStatus, Map<String, String> customTag,
            String updateUser, long updateTime, BucketInfoManager bucketInfoManager) {
        this.name = name;
        this.id = id;
        this.createTime = createTime;
        this.createUser = createUser;
        this.workspace = workspace;
        this.fileTable = fileTable;
        this.bucketInfoMgr = bucketInfoManager;
        this.versionStatus = versionStatus;
        this.customTag = customTag;
        this.updateTime = updateTime;
        this.updateUser = updateUser;
    }

    @Override
    public String toString() {
        return "ScmBucket{" +
                "bucketInfoMgr=" + bucketInfoMgr +
                ", name='" + name + '\'' +
                ", id=" + id +
                ", createTime=" + createTime +
                ", createUser='" + createUser + '\'' +
                ", workspace='" + workspace + '\'' +
                ", fileTable='" + fileTable + '\'' +
                ", versionStatus=" + versionStatus +
                ", customTag=" + customTag + 
                ", updateUser='" + updateUser + '\'' +
                ", updateTime=" + updateTime +
                '}';
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public ScmBucketVersionStatus getVersionStatus() {
        return versionStatus;
    }

    public void setVersionStatus(ScmBucketVersionStatus versionStatus) {
        this.versionStatus = versionStatus;
    }

    public Map<String, String> getCustomTag() {
        return customTag;
    }

    public void setCustomTag(Map<String, String> customTag) {
        this.customTag = customTag;
    }

    public void setFileTable(String fileTable) {
        this.fileTable = fileTable;
    }

    public String getFileTable() {
        return fileTable;
    }

    public MetaAccessor getFileTableAccessor(TransactionContext context) throws ScmServerException, ScmMetasourceException {
        MetaAccessor bucketFileTable = ScmContentModule.getInstance().getMetaService()
                .getMetaSource().createMetaAccessor(fileTable, context);
        return new MetaAccessorBucketWrapper(bucketFileTable, name, bucketInfoMgr);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }
}


// 拦截所有表操作，发生表不存在异常时，按 bucket 不存在处理，通知 bucketInfoManager 清缓存
class MetaAccessorBucketWrapper implements MetaAccessor {
    private final String bucketName;
    private final BucketInfoManager bucketInfoManager;
    private final MetaAccessor bucketFileMetaAccessor;

    public MetaAccessorBucketWrapper(MetaAccessor bucketFileMetaAccessor, String bucketName,
                                     BucketInfoManager bucketInfoManager) {
        this.bucketFileMetaAccessor = bucketFileMetaAccessor;
        this.bucketName = bucketName;
        this.bucketInfoManager = bucketInfoManager;
    }

    private void checkIfBucketTableNotExistError(ScmMetasourceException e)
            throws ScmMetasourceException {
        if (e.getScmError() == ScmError.METASOURCE_TABLE_NOT_EXIST) {
            bucketInfoManager.invalidateBucketCache(bucketName);
            ScmMetasourceException newException = new ScmMetasourceException(
                    "bucket file table not found, assume bucket not exist: bucket=" + bucketName,
                    e);
            newException.setScmError(ScmError.BUCKET_NOT_EXISTS);
            throw newException;
        }
    }

    @Override
    public void insert(BSONObject insertor) throws ScmMetasourceException {
        try {
            bucketFileMetaAccessor.insert(insertor);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public void insert(BSONObject insertor, int flag) throws ScmMetasourceException {
        try {
            bucketFileMetaAccessor.insert(insertor, flag);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws ScmMetasourceException {
        try {
            return bucketFileMetaAccessor.query(matcher, selector, orderBy);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy, long skip,
                            long limit, int flag) throws ScmMetasourceException {
        try {
            return bucketFileMetaAccessor.query(matcher, selector, orderBy, skip, limit, flag);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy,
            BSONObject hint, long skip, long limit, int flag) throws ScmMetasourceException {
        try {
            return bucketFileMetaAccessor.query(matcher, selector, orderBy, hint, skip, limit,
                    flag);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public MetaCursor query(BSONObject matcher, BSONObject selector, BSONObject orderBy, long skip,
                            long limit) throws ScmMetasourceException {
        try {
            return bucketFileMetaAccessor.query(matcher, selector, orderBy, skip, limit);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public long count(BSONObject matcher) throws ScmMetasourceException {
        try {
            return bucketFileMetaAccessor.count(matcher);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public double sum(BSONObject matcher, String field) throws ScmMetasourceException {
        try {
            return bucketFileMetaAccessor.sum(matcher, field);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public BSONObject queryOne(BSONObject matcher, BSONObject selector, BSONObject orderBy)
            throws ScmMetasourceException {
        try {
            return bucketFileMetaAccessor.queryOne(matcher, selector, orderBy);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public void delete(BSONObject deletor) throws ScmMetasourceException {
        try {
            bucketFileMetaAccessor.delete(deletor);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public void delete(BSONObject deletor, BSONObject hint) throws ScmMetasourceException {
        try {
            bucketFileMetaAccessor.delete(deletor, hint);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public BSONObject queryAndDelete(BSONObject deletor) throws ScmMetasourceException {
        try {
            return bucketFileMetaAccessor.queryAndDelete(deletor);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public BSONObject queryAndUpdate(BSONObject matcher, BSONObject updator, BSONObject hint)
            throws ScmMetasourceException {
        try {
            return bucketFileMetaAccessor.queryAndUpdate(matcher, updator, hint);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public void update(BSONObject matcher, BSONObject updator) throws ScmMetasourceException {
        try {
            bucketFileMetaAccessor.update(matcher, updator);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public void update(BSONObject matcher, BSONObject updator, BSONObject hint) throws ScmMetasourceException {
        try {
            bucketFileMetaAccessor.update(matcher, updator, hint);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public BSONObject queryAndUpdate(BSONObject matcher, BSONObject updator, BSONObject hint,
                                     boolean returnNew) throws ScmMetasourceException {
        try {
            return bucketFileMetaAccessor.queryAndUpdate(matcher, updator, hint, returnNew);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }

    @Override
    public void ensureTable(List<String> indexFields, List<String> uniqueIndexField)
            throws ScmMetasourceException {
        bucketFileMetaAccessor.ensureTable(indexFields, uniqueIndexField);
    }

    @Override
    public void ensureTable(List<IndexDef> indexes) throws ScmMetasourceException {
        bucketFileMetaAccessor.ensureTable(indexes);
    }

    @Override
    public void upsert(BSONObject matcher, BSONObject updator) throws ScmMetasourceException {
        try {
            bucketFileMetaAccessor.upsert(matcher, updator);
        }
        catch (ScmMetasourceException e) {
            checkIfBucketTableNotExistError(e);
            throw e;
        }
    }
}
