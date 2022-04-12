package com.sequoiacm.client.core;

import com.sequoiacm.client.dispatcher.BsonReader;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonConverter;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
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
        this.ws = ws;
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
        BSONObject fileInfo = session.getDispatcher().bucketGetFile(name, fileName);
        ScmFileInBucket file = new ScmFileInBucket(this, fileName);
        file.refresh(fileInfo);
        file.setExist(true);
        file.setWorkspace(ws);
        return file;
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
        ScmFileInBucket file = new ScmFileInBucket(this, fileName);
        file.setWorkspace(ws);
        file.setExist(false);
        return file;
    }

    @Override
    public long countFile(BSONObject condition) throws ScmException {
        return session.getDispatcher().bucketCountFile(name, condition);
    }
}
