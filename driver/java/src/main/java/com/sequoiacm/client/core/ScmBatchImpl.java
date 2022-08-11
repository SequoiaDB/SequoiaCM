package com.sequoiacm.client.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.client.element.ScmClassProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTags;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.BsonUtils;

class ScmBatchImpl implements ScmBatch {
    private static final Logger logger = LoggerFactory.getLogger(ScmFileImpl.class);

    private ScmBatchInfo batchInfo;
    private ScmId classId;
    private ScmClassProperties classProperties;
    private ScmTags tags;
    private List<ScmFile> fileList;
    private String createUser;
    private Date createTime;
    private String updateUser;
    private Date updateTime;

    private ScmWorkspace ws;
    private boolean exist = false;

    public ScmBatchImpl(String id) throws ScmException {
        this.batchInfo = new ScmBatchInfo(id);
        this.tags = new ScmTags();
        this.fileList = new ArrayList<ScmFile>();
    }

    public static ScmBatchImpl getInstance(BSONObject bsonObj, ScmWorkspace ws)
            throws ScmException {
        ScmBatchImpl batch = new ScmBatchImpl(null);
        updateScmBatch(batch, bsonObj, ws);
        batch.setWorkspace(ws);
        batch.setExist(true);
        return batch;
    }

    @Override
    public ScmId getId() {
        return this.batchInfo.getId();
    }

    @Override
    public String getName() {
        return this.batchInfo.getName();
    }

    @Override
    public void setName(String name) throws ScmException {
        checkStrNotEmpty(FieldName.Batch.FIELD_NAME, name);
        if (isExist()) {
            BSONObject batchInfo = new BasicBSONObject();
            batchInfo.put(FieldName.Batch.FIELD_NAME, name);
            updateBatchInfo(batchInfo);
        }
        this.batchInfo.setName(name);
    }

    @Override
    public ScmId getClassId() {
        return classId;
    }

    @Override
    public String getCreateUser() {
        return this.createUser;
    }

    @Override
    public Date getCreateTime() {
        return this.createTime;
    }

    @Override
    public String getUpdateUser() {
        return this.updateUser;
    }

    @Override
    public Date getUpdateTime() {
        return this.updateTime;
    }

    @Override
    public String getWorkspaceName() {
        return this.ws.getName();
    }

    @Override
    public ScmClassProperties getClassProperties() {
        return this.classProperties;
    }

    @Override
    public void setClassProperty(String key, Object value) throws ScmException {
        if (null == classId) {
            throw new ScmException(ScmError.BATCH_CLASS_UNDEFINED,
                    "The batch does not specify a class");
        }

        checkArgNotNull(FieldName.Batch.FIELD_CLASS_PROPERTIES + "'s key", key);
        if (isExist()) {
            BSONObject batchInfo = new BasicBSONObject();
            batchInfo.put(FieldName.Batch.FIELD_CLASS_PROPERTIES + "." + key, value);
            updateBatchInfo(batchInfo);
        }
        this.classProperties.addProperty(key, value);
    }

    @Override
    public void setClassProperties(ScmClassProperties classProperties) throws ScmException {
        if (null == classProperties) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "batch class properties cannot be set null");
        }
        if (isExist()) {
            BSONObject batchInfo = new BasicBSONObject();
            BSONObject prop = new BasicBSONObject();
            String cid = classProperties.getClassId();
            Set<String> keySet = classProperties.keySet();
            for (String key : keySet) {
                checkArgNotNull(FieldName.Batch.FIELD_CLASS_PROPERTIES + "'s key", key);
                prop.put(key, classProperties.getProperty(key));
            }
            batchInfo.put(FieldName.Batch.FIELD_CLASS_ID, cid);
            batchInfo.put(FieldName.Batch.FIELD_CLASS_PROPERTIES, prop);
            updateBatchInfo(batchInfo);
        }

        this.classId = new ScmId(classProperties.getClassId(), false);
        this.classProperties = classProperties;
    }

    @Override
    public ScmTags getTags() {
        return this.tags;
    }

    @Override
    public void setTags(ScmTags tags) throws ScmException {
        if (tags == null) {
            tags = new ScmTags();
        }
        if (isExist()) {
            BSONObject batchInfo = new BasicBSONObject();
            batchInfo.put(FieldName.Batch.FIELD_TAGS, tags.toSet());
            updateBatchInfo(batchInfo);
        }
        this.tags = tags;
    }

    @Override
    public void addTag(String tag) throws ScmException {
        tags.addTag(tag);
        if (isExist()) {
            setTags(tags);
        }
    }

    @Override
    public void removeTag(String tag) throws ScmException {
        tags.removeTag(tag);
        if (isExist()) {
            setTags(tags);
        }
    }

    @Override
    public List<ScmFile> listFiles() throws ScmException {
        if (!isExist()) {
            throw new ScmException(ScmError.OPERATION_UNSUPPORTED,
                    "batch is unexist, listFiles operation cannot be done");
        }
        return this.fileList;
    }

    @Override
    public ScmId save() throws ScmException {
        if (isExist()) {
            throw new ScmException(ScmError.OPERATION_UNSUPPORTED,
                    "batch already exists, save operation can't be done repeatedly");
        }

        BSONObject info = new BasicBSONObject();

        if (getId() != null) {
            info.put(FieldName.Batch.FIELD_ID, getId().get());
        }

        checkStrNotEmpty(FieldName.Batch.FIELD_NAME, getName());
        info.put(FieldName.Batch.FIELD_NAME, getName());

        if (classProperties != null) {
            info.put(FieldName.Batch.FIELD_CLASS_ID, classProperties.getClassId());
            String propFieldName = FieldName.Batch.FIELD_CLASS_PROPERTIES;
            info.put(propFieldName, convertToBson(this.classProperties.toMap(), propFieldName));
        }

        if (tags != null) {
            info.put(FieldName.Batch.FIELD_TAGS, tags.toSet());
        }

        BSONObject batchInfo = getSession().getDispatcher().createBatch(getWorkspaceName(), info);

        updateScmBatch(this, batchInfo, this.ws);
        setExist(true);

        return getId();
    }

    @Override
    public void delete() throws ScmException {
        if (!isExist()) {
            throw new ScmException(ScmError.OPERATION_UNSUPPORTED,
                    "batch is unexist, delete operation cannot be done");
        }
        getSession().getDispatcher().deleteBatch(getWorkspaceName(), getId().get());
        setExist(false);
    }

    @Override
    public void attachFile(ScmId fileId) throws ScmException {
        if (!isExist()) {
            throw new ScmException(ScmError.OPERATION_UNSUPPORTED,
                    "batch is unexist, attachFile operation cannot be done");
        }

        if (null == fileId) {
            throw new ScmException(ScmError.INVALID_ID, "the file to be attached cannot be null");
        }

        ScmFile file = ScmFactory.File.getInstance(ws, fileId);
        getSession().getDispatcher().batchAttachFile(getWorkspaceName(), getId().get(),
                fileId.get());
        // add file to fileList
        addFile(file);
    }

    @Override
    public void detachFile(ScmId fileId) throws ScmException {
        if (!isExist()) {
            throw new ScmException(ScmError.OPERATION_UNSUPPORTED,
                    "batch is unexist, detachFile operation cannot be done");
        }

        if (null == fileId) {
            throw new ScmException(ScmError.INVALID_ID, "the file to be detached cannot be null");
        }

        getSession().getDispatcher().batchDetachFile(getWorkspaceName(), getId().get(),
                fileId.get());

        // remove file from fileList
        Iterator<ScmFile> iterator = fileList.iterator();
        while (iterator.hasNext()) {
            ScmFile file = iterator.next();
            String iterId = file.getFileId().get();
            String delId = fileId.get();
            if (iterId.equals(delId)) {
                iterator.remove();
                break;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        buf.append("id : \"" + batchInfo.getId().get() + "\" , ");
        buf.append("name : \"" + batchInfo.getName() + "\" , ");
        buf.append("workspaceName : \"" + ws.getName() + "\" , ");
        buf.append("properties : " + classProperties + " , ");
        buf.append("tags : " + tags + " , ");
        buf.append("createUser : \"" + createUser + "\", ");
        buf.append("createTime : \"" + createTime + "\" , ");
        buf.append("updateUser : \"" + updateUser + "\" , ");
        buf.append("updateTime : \"" + updateTime + "\" , ");
        buf.append("files : " + fileList);
        buf.append("}");
        return buf.toString();
    }

    public boolean isExist() {
        return exist;
    }

    private BSONObject convertToBson(Map<String, Object> map, String fieldName)
            throws ScmException {
        BSONObject prop = new BasicBSONObject();
        Set<String> keySet = map.keySet();
        for (String key : keySet) {
            checkArgNotNull(fieldName + "'s key", key);
            prop.put(key, map.get(key));
        }
        return prop;
    }

    private void checkStrNotEmpty(String field, String val) throws ScmException {
        if (null == val || val.trim().length() == 0) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "batch '" + field + "' can't be null or an empty string");
        }
    }

    private void checkArgNotNull(String field, Object val) throws ScmException {
        if (null == val) {
            throw new ScmException(ScmError.INVALID_ARGUMENT, "batch " + field + " cannot be null");
        }
    }

    private static void updateScmBatch(ScmBatchImpl batch, BSONObject bsonObj, ScmWorkspace ws)
            throws ScmException {
        Object obj;
        obj = bsonObj.get(FieldName.Batch.FIELD_ID);
        if (null != obj) {
            batch.setId(new ScmId((String) obj, false));
        }

        obj = bsonObj.get(FieldName.Batch.FIELD_NAME);
        if (null != obj) {
            batch.setName((String) obj);
        }

        // class properties
        String cid = (String) bsonObj.get(FieldName.Batch.FIELD_CLASS_ID);
        BSONObject prop = (BSONObject) bsonObj.get(FieldName.Batch.FIELD_CLASS_PROPERTIES);
        if (cid != null && prop != null) {
            ScmClassProperties properties = new ScmClassProperties(cid);
            Set<String> proKeySet = prop.keySet();
            for (String proKey : proKeySet) {
                properties.addProperty(proKey, prop.get(proKey));
            }
            batch.setClassProperties(properties);
        }

        // tags
        obj = bsonObj.get(FieldName.Batch.FIELD_TAGS);
        if (null != obj) {
            ScmTags tags = new ScmTags();
            BasicBSONList bsonList = null;

            try {
                bsonList = (BasicBSONList) obj;
                for (Object tag : bsonList) {
                    tags.addTag((String) tag);
                }
            }
            catch (ClassCastException e) {
                // tags compatibility processing : { "tagkey" : "tagvalue"} cast
                // to ["tagkey : tagvalue"]
                BSONObject bsonMap = (BSONObject) obj;
                Set<String> tagKeys = bsonMap.keySet();
                for (String tagKey : tagKeys) {
                    tags.addTag(tagKey + " : " + bsonMap.get(tagKey));
                }
                logger.warn("tags compatibility processing: old tags map={}, new tags set={}", obj,
                        bsonList);
            }
            batch.setTags(tags);
        }

        obj = bsonObj.get(FieldName.Batch.FIELD_INNER_CREATE_USER);
        if (null != obj) {
            batch.setCreateUser((String) obj);
        }

        // createTime
        Number createTimeNumber = BsonUtils.getNumber(bsonObj,
                FieldName.Batch.FIELD_INNER_CREATE_TIME);
        if (createTimeNumber != null) {
            batch.setCreateTime(new Date(createTimeNumber.longValue()));
        }

        obj = bsonObj.get(FieldName.Batch.FIELD_INNER_UPDATE_USER);
        if (null != obj) {
            batch.setUpdateUser((String) obj);
        }

        // updateTime
        Number updateTimeNumber = BsonUtils.getNumber(bsonObj,
                FieldName.Batch.FIELD_INNER_UPDATE_TIME);
        if (updateTimeNumber != null) {
            batch.setUpdateTime(new Date(updateTimeNumber.longValue()));
        }

        // files
        obj = bsonObj.get(FieldName.Batch.FIELD_FILES);
        if (null != obj) {
            BasicBSONList files = (BasicBSONList) obj;
            for (Object fObj : files) {
                BSONObject fBson = (BSONObject) fObj;
                ScmFileImpl file = ScmFileImpl.getInstanceByBSONObject(fBson);
                file.setWorkspace(ws);
                file.setExist(true);
                batch.addFile(file);
            }
        }
    }

    private void updateBatchInfo(BSONObject batchInfo) throws ScmException {
        getSession().getDispatcher().updateBatchInfo(getWorkspaceName(), getId().get(), batchInfo);
        // TODO
        // the rest api of updateBatchInfo returns nothing in current version
        // this.updateTime = new Date(result.getUpdateTime());
        this.updateUser = getSession().getUser();

    }

    void setWorkspace(ScmWorkspace ws) {
        this.ws = ws;
    }

    void setExist(boolean exist) {
        this.exist = exist;
    }

    void addFile(ScmFile file) {
        this.fileList.add(file);
    }

    void setCreateUser(String user) {
        this.createUser = user;
    }

    void setCreateTime(Date date) {
        this.createTime = date;
    }

    void setUpdateUser(String user) {
        this.updateUser = user;
    }

    void setUpdateTime(Date date) {
        this.updateTime = date;
    }

    void setId(ScmId id) {
        this.batchInfo.setId(id);
    }

    ScmWorkspace getWorkspace() {
        return this.ws;
    }

    ScmSession getSession() {
        return ws.getSession();
    }

}
