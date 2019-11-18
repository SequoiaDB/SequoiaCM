package com.sequoiacm.client.core;

import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.core.ScmFactory.Directory;
import com.sequoiacm.client.core.ScmFactory.File;
import com.sequoiacm.client.dispatcher.BsonReader;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.client.util.BsonConverter;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmArgChecker;

class ScmDirectoryImpl extends ScmDirectory {
    private ScmWorkspace ws;
    private String name;
    private String id;
    private String parentId;
    private String user;
    private String updateUser;
    private Date updateDate;
    private Date createDate;
    private ScmSession ss;

    ScmDirectoryImpl(ScmWorkspace ws, BSONObject dirInfo) throws ScmException {
        try {
            this.ss = ws.getSession();
            this.ws = ws;
            init(dirInfo);
        }
        catch (ScmException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException("failed to create ScmDirectoryImpl:record=" + dirInfo, e);
        }
    }

    private void init(BSONObject dirInfo) throws ScmException {
        Object obj = getValueCheckNotNull(dirInfo, FieldName.FIELD_CLDIR_CREATE_TIME);
        createDate = new Date((Long) obj);

        obj = getValueCheckNotNull(dirInfo, FieldName.FIELD_CLDIR_ID);
        id = (String) obj;

        obj = getValueCheckNotNull(dirInfo, FieldName.FIELD_CLDIR_NAME);
        name = (String) obj;

        obj = getValueCheckNotNull(dirInfo, FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID);
        parentId = (String) obj;

        obj = getValueCheckNotNull(dirInfo, FieldName.FIELD_CLDIR_UPDATE_TIME);
        updateDate = new Date((Long) obj);

        obj = getValueCheckNotNull(dirInfo, FieldName.FIELD_CLDIR_UPDATE_USER);
        updateUser = (String) obj;

        obj = getValueCheckNotNull(dirInfo, FieldName.FIELD_CLDIR_USER);
        user = (String) obj;
    }

    private Object getValueCheckNotNull(BSONObject dirInfo, String key) throws ScmException {
        Object value = dirInfo.get(key);
        if (value == null) {
            throw new ScmSystemException(
                    "directory record missing key:record=" + dirInfo + ",key=" + key);
        }
        return value;
    }

    @Override
    ScmWorkspace getWorkspace() throws ScmException {
        return ws;
    }

    @Override
    public String getName() throws ScmException {
        return name;
    }

    @Override
    public ScmDirectory getParentDirectory() throws ScmException {
        BSONObject parentDir = ss.getDispatcher().getDir(ws.getName(), parentId, null);
        return new ScmDirectoryImpl(ws, parentDir);
    }

    @Override
    public ScmDirectory getSubdirectory(String name) throws ScmException {
        if (name == null) {
            throw new ScmInvalidArgumentException("invalid arg:name=null");
        }
        BasicBSONObject condition = new BasicBSONObject();
        condition.put(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID, id);
        condition.put(FieldName.FIELD_CLDIR_NAME, name);
        ScmCursor<ScmDirectory> c = Directory.listInstance(ws, condition);
        try {
            if (c.hasNext()) {
                return c.getNext();
            }
            return null;
        }
        finally {
            c.close();
        }
    }

    @Override
    public ScmFile getSubfile(String name) throws ScmException {
        if (name == null) {
            throw new ScmInvalidArgumentException("invalid arg:name=null");
        }
        BasicBSONObject condition = new BasicBSONObject();
        condition.put(FieldName.FIELD_CLFILE_NAME, name);

        ScmCursor<ScmFileBasicInfo> c = listFiles(condition);
        try {
            if (c.hasNext()) {
                return File.getInstance(ws, c.getNext().getFileId());
            }
            return null;
        }
        finally {
            c.close();
        }
    }

    @Override
    public String getUser() throws ScmException {
        return user;
    }

    @Override
    public Date getCreateTime() throws ScmException {
        return createDate;
    }

    @Override
    public Date getUpdateTime() throws ScmException {
        return updateDate;
    }

    @Override
    public String getUpdateUser() throws ScmException {
        return updateUser;
    }

    @Override
    public String getWorkspaceName() throws ScmException {
        return ws.getName();
    }

    @Override
    public void rename(String newname) throws ScmException {
        if (!ScmArgChecker.Directory.checkDirectoryName(newname)) {
            throw new ScmInvalidArgumentException("invlid arg:newname=" + newname);
        }
        long updateTime = ss.getDispatcher().renameDir(ws.getName(), id, newname);
        updateDate = new Date(updateTime);
        name = newname;
    }

    @Override
    public void move(ScmDirectory newParentDir) throws ScmException {
        if (newParentDir == null) {
            throw new ScmInvalidArgumentException("invlid arg:newPrarentDir=null");
        }
        long updateTime = ss.getDispatcher().moveDir(ws.getName(), id, newParentDir.getId());
        updateDate = new Date(updateTime);
        parentId = newParentDir.getId();
    }

    @Override
    public ScmCursor<ScmFileBasicInfo> listFiles(BSONObject condition) throws ScmException {
        return listFiles(condition, 0, -1, null);
    }

    @Override
    public ScmCursor<ScmDirectory> listDirectories(BSONObject condition) throws ScmException {
        BSONObject parentIdCondition = ScmQueryBuilder
                .start(FieldName.FIELD_CLDIR_PARENT_DIRECTORY_ID).is(id).get();
        BSONObject andCondition;
        if (condition != null) {
            andCondition = ScmQueryBuilder.start().and(condition, parentIdCondition).get();
        }
        else {
            andCondition = parentIdCondition;
        }
        return Directory.listInstance(ws, andCondition);
    }

    @Override
    public ScmDirectory createSubdirectory(String name) throws ScmException {
        if (!ScmArgChecker.Directory.checkDirectoryName(name)) {
            throw new ScmInvalidArgumentException("invlid arg:name=" + name);
        }
        BSONObject dir = ss.getDispatcher().createDir(ws.getName(), name, id, null);
        return new ScmDirectoryImpl(ws, dir);
    }

    @Override
    public String getPath() throws ScmException {
        return ss.getDispatcher().getPath(ws.getName(), id);
    }

    @Override
    public void delete() throws ScmException {
        ss.getDispatcher().deleteDir(ws.getName(), id, null);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ScmDirectoryImpl [ws=" + ws.getName() + ", name=" + name + ", id=" + id
                + ", parentId=" + parentId + ", user=" + user + ", updateUser=" + updateUser
                + ", updateDate=" + updateDate + ", createDate=" + createDate + "]";
    }

    @Override
    public ScmCursor<ScmFileBasicInfo> listFiles(BSONObject condition, int skip, int limit,
            BSONObject orderby) throws ScmException {
        if (skip < 0) {
            throw new ScmInvalidArgumentException(
                    "skip must be greater than or equals to 0:skip=" + skip);
        }
        if (limit < -1) {
            throw new ScmInvalidArgumentException(
                    "limit must be greater than or equals to -1:limit=" + limit);
        }

        BsonReader reader = ws.getSession().getDispatcher().getDirFileList(ws.getName(), getId(),
                condition, skip, limit, orderby);
        ScmCursor<ScmFileBasicInfo> fbiCursor = new ScmBsonCursor<ScmFileBasicInfo>(reader,
                new BsonConverter<ScmFileBasicInfo>() {
                    @Override
                    public ScmFileBasicInfo convert(BSONObject obj) throws ScmException {
                        return new ScmFileBasicInfo(obj);
                    }
                });

        return fbiCursor;
    }

}
