package com.sequoiacm.client.core;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.dispatcher.BsonReader;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.BsonConverter;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.client.util.ScmHelper;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.Collections;
import java.util.Date;
import java.util.TreeMap;
import java.util.Map;

class ScmBucketImpl implements ScmBucket {
    private ScmWorkspace ws;
    private ScmSession session;
    private String name;
    private long id;
    private String wsName;
    private String user;
    private Date createTime;
    private ScmBucketVersionStatus versionStatus;
    private Date updateTime;
    private String updateUser;
    private Map<String, String> customTag;

    public ScmBucketImpl(ScmSession session, BSONObject obj) throws ScmException {
        ScmWorkspace workspace = ScmFactory.Workspace
                .getWorkspace(BsonUtils.getStringChecked(obj, FieldName.Bucket.WORKSPACE), session);
        init(workspace, obj);
    }

    public ScmBucketImpl(ScmWorkspace ws, BSONObject obj) throws ScmException {
        init(ws, obj);
    }

    private void init(ScmWorkspace ws, BSONObject obj) throws ScmException {
        this.session = ws.getSession();
        name = BsonUtils.getStringChecked(obj, FieldName.Bucket.NAME);
        id = BsonUtils.getNumberChecked(obj, FieldName.Bucket.ID).longValue();
        wsName = BsonUtils.getStringChecked(obj, FieldName.Bucket.WORKSPACE);
        user = BsonUtils.getStringChecked(obj, FieldName.Bucket.CREATE_USER);
        long timeLong = BsonUtils.getNumberChecked(obj, FieldName.Bucket.CREATE_TIME).longValue();
        this.createTime = new Date(timeLong);
        timeLong = BsonUtils.getNumberChecked(obj, FieldName.Bucket.UPDATE_TIME).longValue();
        this.updateTime = new Date(timeLong);
        updateUser = BsonUtils.getStringChecked(obj, FieldName.Bucket.UPDATE_USER);
        this.ws = ws;
        BSONObject customTagObj = BsonUtils.getBSONObject(obj, FieldName.FIELD_CLFILE_CUSTOM_TAG);
        this.customTag = ScmHelper.parseCustomTag(customTagObj);
        String versionStatusStr = BsonUtils.getStringChecked(obj, FieldName.Bucket.VERSION_STATUS);
        this.versionStatus = ScmBucketVersionStatus.parse(versionStatusStr);
        if (versionStatus == null) {
            throw new ScmException(ScmError.SYSTEM_ERROR, "unknown version status: bucket=" + name
                    + ", versionStatus=" + versionStatusStr);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getWorkspace() {
        return wsName;
    }

    @Override
    public String getCreateUser() {
        return user;
    }

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public ScmFile getFile(String fileName) throws ScmException {
        if (Strings.isEmpty(fileName)) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "The fileName is null or empty" + fileName);
        }
        return getFile(fileName, -1, -1);
    }

    @Override
    public ScmFile getFile(String fileName, int majorVersion, int minorVersion)
            throws ScmException {
        if (Strings.isEmpty(fileName)) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "The fileName is null or empty" + fileName);
        }
        BSONObject fileInfo = session.getDispatcher().bucketGetFile(name, fileName, majorVersion,
                minorVersion);
        return createFileInstance(fileName, fileInfo);
    }

    private ScmFile createFileInstance(String fileName, BSONObject fileInfo) throws ScmException {
        ScmFileInBucket file = new ScmFileInBucket(this.getName(), this.getId(), fileName);
        file.refresh(fileInfo);
        file.setExist(true);
        file.setWorkspace(ws);
        return file;
    }

    @Override
    public ScmFile getNullVersionFile(String fileName) throws ScmException {
        if (Strings.isEmpty(fileName)) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "The fileName is null or empty" + fileName);
        }
        BSONObject fileInfo = session.getDispatcher().bucketGetFileNullVersion(name,
                fileName);
        return createFileInstance(fileName, fileInfo);
    }

    @Override
    public ScmCursor<ScmFileBasicInfo> listFile(BSONObject condition, BSONObject orderby, long skip,
            long limit) throws ScmException {
        return internalListFile(null, condition, orderby, skip, limit, false);
    }

    @Override
    public ScmCursor<ScmFileBasicInfo> listFile(ScopeType scope, BSONObject condition,
            BSONObject orderby, long skip, long limit) throws ScmException {
        return internalListFile(scope, condition, orderby, skip, limit, true);
    }

    private ScmCursor<ScmFileBasicInfo> internalListFile(ScopeType scope, BSONObject condition,
            BSONObject orderby, long skip, long limit, boolean isNeedCheckSopce)
            throws ScmException {
        if (isNeedCheckSopce && scope == null) {
            throw new ScmInvalidArgumentException("scope is null");
        }
        BsonReader reader = session.getDispatcher().bucketListFile(name, scope, condition, orderby,
                skip, limit);
        return new ScmBsonCursor<ScmFileBasicInfo>(reader, new BsonConverter<ScmFileBasicInfo>() {
            @Override
            public ScmFileBasicInfo convert(BSONObject obj) throws ScmException {
                return new ScmFileBasicInfo(obj);
            }
        });
    }

    @Override
    public ScmFile createFile(String fileName) throws ScmException {
        ScmFileInBucket file = new ScmFileInBucket(this.getName(), this.getId(), fileName);
        file.setWorkspace(ws);
        file.setExist(false);
        return file;
    }

    @Override
    public long countFile(BSONObject condition) throws ScmException {
        return internalCountFile(null, condition, false);
    }

    @Override
    public long countFile(ScopeType scope, BSONObject condition) throws ScmException {
        return internalCountFile(scope, condition, true);
    }

    private long internalCountFile(ScopeType scope, BSONObject condition, boolean isNeedCheckSopce)
            throws ScmException {
        if (isNeedCheckSopce && null == scope) {
            throw new ScmInvalidArgumentException("scope is null");
        }
        return session.getDispatcher().bucketCountFile(name, scope, condition);
    }

    @Override
    public void deleteFileVersion(String fileName, int majorVersion, int minorVersion)
            throws ScmException {
        if (Strings.isEmpty(fileName)) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "The fileName is null or empty" + fileName);
        }
        session.getDispatcher().bucketDeleteFileVersion(this.name, fileName, majorVersion,
                minorVersion);
    }

    @Override
    public void deleteFile(String fileName, boolean isPhysical) throws ScmException {
        if (Strings.isEmpty(fileName)) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "The fileName is null or empty" + fileName);
        }
        session.getDispatcher().bucketDeleteFile(this.name, fileName, isPhysical);
    }

    @Override
    public ScmBucketVersionStatus getVersionStatus() {
        return versionStatus;
    }

    @Override
    public void enableVersionControl() throws ScmException {
        session.getDispatcher().setBucketVersionStatus(name, ScmBucketVersionStatus.Enabled);
        this.versionStatus = ScmBucketVersionStatus.Enabled;
    }

    @Override
    public void suspendVersionControl() throws ScmException {
        session.getDispatcher().setBucketVersionStatus(name, ScmBucketVersionStatus.Suspended);
        this.versionStatus = ScmBucketVersionStatus.Suspended;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    @Override
    public void setCustomTag(Map<String, String> customTag) throws ScmException {
        customTag = customTag == null ? Collections.<String, String> emptyMap() : customTag;
        // 对于不能包含 null 键的 map 集合，使用 containsKey() 方法会报错空指针（如：TreeMap），因此这里通过遍历的方式来判断
        for (Map.Entry<String, String> entry : customTag.entrySet()) {
            if (entry.getKey() == null) {
                throw new ScmException(ScmError.BUCKET_INVALID_CUSTOMTAG,
                        "the customTag key is null");
            }
        }
        session.getDispatcher().setBucketTag(name, customTag);
        this.customTag = new TreeMap<String, String>(customTag);
    }

    @Override
    public Map<String, String> getCustomTag() {
        // 参照标准 s3，桶未设置标签，返回 null
        if (customTag == null || customTag.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableMap(customTag);
    }

    @Override
    public void deleteCustomTag() throws ScmException {
        session.getDispatcher().deleteBucketTag(name);
        this.customTag.clear();
    }
}
