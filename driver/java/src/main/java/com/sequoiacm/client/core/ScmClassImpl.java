package com.sequoiacm.client.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.client.element.ScmClassBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;

class ScmClassImpl implements ScmClass {

    private ScmClassBasicInfo classInfo;
    private String updateUser;
    private Date updateTime;
    private List<ScmAttribute> attrList;

    private ScmWorkspace ws;
    private boolean exist;

    ScmClassImpl() {
        classInfo = new ScmClassBasicInfo();
        attrList = new ArrayList<ScmAttribute>();
    }

    ScmClassImpl(ScmWorkspace ws, BSONObject classInfo) throws ScmException {
        this();
        setWorkspace(ws);
        refresh(classInfo);
        setExist(true);
    }

    private void setId(ScmId classId) {
        this.classInfo.setId(classId);
    }

    private void setExist(boolean exist) {
        this.exist = exist;
    }

    @Override
    public boolean isExist() {
        return this.exist;
    }

    private void refresh(BSONObject classBSON) throws ScmException {
        boolean isNeedResetExist = false;
        if (isExist()) {
            setExist(false);
            isNeedResetExist = true;
        }

        Object obj;
        obj = classBSON.get(FieldName.Class.FIELD_ID);
        if (null != obj) {
            setId(new ScmId((String) obj, false));
        }

        obj = classBSON.get(FieldName.Class.FIELD_NAME);
        if (null != obj) {
            setName((String) obj);
        }

        obj = classBSON.get(FieldName.Class.FIELD_DESCRIPTION);
        if (null != obj) {
            setDescription((String) obj);
        }

        obj = classBSON.get(FieldName.Class.FIELD_INNER_CREATE_USER);
        if (null != obj) {
            setCreateUser((String) obj);
        }

        obj = classBSON.get(FieldName.Class.FIELD_INNER_CREATE_TIME);
        if (null != obj) {
            Long ts = (Long) obj;
            setCreateTime(new Date(ts));
        }

        obj = classBSON.get(FieldName.Class.FIELD_INNER_UPDATE_USER);
        if (null != obj) {
            setUpdateUser((String) obj);
        }

        obj = classBSON.get(FieldName.Class.FIELD_INNER_UPDATE_TIME);
        if (null != obj) {
            Long ts = (Long) obj;
            setUpdateTime(new Date(ts));
        }

        // attrs
        obj = classBSON.get(FieldName.Class.REL_ATTR_INFOS);
        if (null != obj) {
            BasicBSONList attrs = (BasicBSONList) obj;
            for (Object aObj : attrs) {
                BSONObject aBson = (BSONObject) aObj;
                ScmAttributeImpl attr = new ScmAttributeImpl(ws, aBson);
                this.attrList.add(attr);
            }
        }

        if (isNeedResetExist) {
            setExist(true);
        }
    }

    @Override
    public ScmId getId() {
        return this.classInfo.getId();
    }

    @Override
    public String getName() {
        return this.classInfo.getName();
    }

    @Override
    public void setName(String className) throws ScmException {
        checkStrNotEmpty(FieldName.Class.FIELD_NAME, className);
        if (isExist()) {
            BSONObject classInfo = new BasicBSONObject();
            classInfo.put(FieldName.Class.FIELD_NAME, className);
            updateClassInfo(classInfo);
        }
        this.classInfo.setName(className);
    }

    @Override
    public String getDescription() {
        return this.classInfo.getDescription();
    }

    @Override
    public void setDescription(String desc) throws ScmException {
        if (desc == null) {
            throw new ScmException(ScmError.INVALID_ARGUMENT, "class 'description' cannot be null");
        }
        if (isExist()) {
            BSONObject classInfo = new BasicBSONObject();
            classInfo.put(FieldName.Class.FIELD_DESCRIPTION, desc);
            updateClassInfo(classInfo);
        }
        this.classInfo.setDescription(desc);
    }

    @Override
    public String getCreateUser() {
        return this.classInfo.getCreateUser();
    }

    private void setCreateUser(String user) throws ScmException {
        this.classInfo.setCreateUser(user);
    }

    @Override
    public Date getCreateTime() {
        return this.classInfo.getCreateTime();
    }

    private void setCreateTime(Date date) {
        this.classInfo.setCreateTime(date);
    }

    @Override
    public String getUpdateUser() {
        return this.updateUser;
    }

    private void setUpdateUser(String user) throws ScmException {
        this.updateUser = user;
    }

    @Override
    public Date getUpdateTime() {
        return this.updateTime;
    }

    private void setUpdateTime(Date date) {
        this.updateTime = date;
    }

    private void checkStrNotEmpty(String field, String val) throws ScmException {
        if (null == val || val.trim().length() == 0) {
            throw new ScmException(ScmError.INVALID_ARGUMENT,
                    "class '" + field + "' cannot be null or an empty string");
        }
    }

    private void updateClassInfo(BSONObject updateInfo) throws ScmException {
        BSONObject classInfo = getSession().getDispatcher().updateClassInfo(this.ws.getName(),
                getId(), updateInfo);
        refresh(classInfo);
    }

    @Override
    public List<ScmAttribute> listAttrs() {
        return this.attrList;
    }

    @Override
    public void delete() throws ScmException {
        if (isExist()) {
            getSession().getDispatcher().deleteClass(this.ws.getName(), getId());
            setExist(false);
        }
    }

    void save() throws ScmException {
        BSONObject info = new BasicBSONObject();
        checkStrNotEmpty(FieldName.Class.FIELD_NAME, getName());
        info.put(FieldName.Class.FIELD_NAME, getName());
        info.put(FieldName.Class.FIELD_DESCRIPTION, getDescription());

        BSONObject classInfo = getSession().getDispatcher().createClass(this.ws.getName(), info);

        refresh(classInfo);
        setExist(true);
    }

    @Override
    public void attachAttr(ScmId attrId) throws ScmException {
        if (!isExist()) {
            throw new ScmException(ScmError.OPERATION_UNSUPPORTED,
                    "class is unexist, attachAttr operation cannot be done");
        }

        if (null == attrId) {
            throw new ScmException(ScmError.INVALID_ID,
                    "the attribute to be attached cannot be null");
        }

        getSession().getDispatcher().classAttachAttr(this.ws.getName(), getId(), attrId);

        // add attr to attrList
        ScmAttribute attr = ScmFactory.Attribute.getInstance(ws, attrId);
        this.attrList.add(attr);
    }

    @Override
    public void detachAttr(ScmId attrId) throws ScmException {
        if (!isExist()) {
            throw new ScmException(ScmError.OPERATION_UNSUPPORTED,
                    "class is unexist, detachAttr operation cannot be done");
        }

        if (null == attrId) {
            throw new ScmException(ScmError.INVALID_ID,
                    "the attribute to be detached cannot be null");
        }

        getSession().getDispatcher().classDetachAttr(this.ws.getName(), getId(), attrId);

        // remove attr from attrList
        Iterator<ScmAttribute> iterator = attrList.iterator();
        while (iterator.hasNext()) {
            ScmAttribute attr = iterator.next();
            String iterId = attr.getId().get();
            String delId = attrId.get();
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
        buf.append("id : \"" + classInfo.getId().get() + "\", ");
        buf.append("name : \"" + classInfo.getName() + "\", ");
        buf.append("description : \"" + classInfo.getDescription() + "\", ");
        buf.append("createUser : \"" + classInfo.getCreateUser() + "\", ");
        buf.append("createTime : " + classInfo.getCreateTime().getTime() + ", ");
        buf.append("attrs : " + attrList);
        buf.append("}");
        return buf.toString();
    }

    @Override
    public ScmWorkspace getWorkspace() {
        return this.ws;
    }

    void setWorkspace(ScmWorkspace ws) {
        this.ws = ws;
    }

    private ScmSession getSession() {
        return ws.getSession();
    }

}
