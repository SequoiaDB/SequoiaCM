package com.sequoiacm.client.core;

import com.sequoiacm.client.dispatcher.BsonReader;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonConverter;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.module.ScmBucketVersionStatus;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;

import java.util.Date;

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
        BsonReader reader = session.getDispatcher().bucketListFile(name, condition, orderby, skip,
                limit);
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
        return session.getDispatcher().bucketCountFile(name, condition);
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
}
