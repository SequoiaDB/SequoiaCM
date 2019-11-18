package com.sequoiacm.client.core;

import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.metadata.ScmAttrRule;
import com.sequoiacm.client.element.metadata.ScmDoubleRule;
import com.sequoiacm.client.element.metadata.ScmIntegerRule;
import com.sequoiacm.client.element.metadata.ScmStringRule;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.AttributeType;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;

class ScmAttributeImpl implements ScmAttribute {

    private ScmId id;
    private String name;
    private String displayName = "";
    private String description = "";
    private AttributeType type;
    private ScmAttrRule checkRule;
    private boolean required = false;
    private String createUser;
    private Date createTime;
    private String updateUser;
    private Date updateTime;

    private ScmWorkspace ws;
    private boolean exist;

    ScmAttributeImpl() {
    }

    ScmAttributeImpl(ScmWorkspace ws, BSONObject attrInfo) throws ScmException {
        this();
        setWorkspace(ws);
        refresh(attrInfo);
        setExist(true);
    }

    private void setId(ScmId attrId) {
        this.id = attrId;
    }

    private void setExist(boolean exist) {
        this.exist = exist;
    }

    @Override
    public boolean isExist() {
        return this.exist;
    }

    private void refresh(BSONObject attrBSON) throws ScmException {
        boolean isNeedResetExist = false;
        if (isExist()) {
            setExist(false);
            isNeedResetExist = true;
        }

        Object obj;
        obj = attrBSON.get(FieldName.Attribute.FIELD_ID);
        if (null != obj) {
            setId(new ScmId((String) obj, false));
        }

        obj = attrBSON.get(FieldName.Attribute.FIELD_NAME);
        if (null != obj) {
            setName((String) obj);
        }

        obj = attrBSON.get(FieldName.Attribute.FIELD_DISPLAY_NAME);
        if (null != obj) {
            setDisplayName((String) obj);
        }

        obj = attrBSON.get(FieldName.Attribute.FIELD_DESCRIPTION);
        if (null != obj) {
            setDescription((String) obj);
        }

        obj = attrBSON.get(FieldName.Attribute.FIELD_TYPE);
        if (null != obj) {
            this.type = AttributeType.getType((String) obj);
        }

        obj = attrBSON.get(FieldName.Attribute.FIELD_CHECK_RULE);
        if (null != obj) {
            BSONObject ruleObj = (BSONObject) obj;
            ScmAttrRule rule = null;
            switch (this.type) {
                case INTEGER:
                    rule = new ScmIntegerRule(ruleObj);
                    break;

                case DOUBLE:
                    rule = new ScmDoubleRule(ruleObj);
                    break;

                case STRING:
                    rule = new ScmStringRule(ruleObj);
                    break;

                default:
                    break;
            }
            setCheckRule(rule);
        }

        obj = attrBSON.get(FieldName.Attribute.FIELD_REQUIRED);
        if (null != obj) {
            setRequired((Boolean) obj);
        }

        obj = attrBSON.get(FieldName.Attribute.FIELD_INNER_CREATE_USER);
        if (null != obj) {
            setCreateUser((String) obj);
        }

        obj = attrBSON.get(FieldName.Attribute.FIELD_INNER_CREATE_TIME);
        if (null != obj) {
            Long ts = (Long) obj;
            setCreateTime(new Date(ts));
        }

        obj = attrBSON.get(FieldName.Attribute.FIELD_INNER_UPDATE_USER);
        if (null != obj) {
            setUpdateUser((String) obj);
        }

        obj = attrBSON.get(FieldName.Attribute.FIELD_INNER_UPDATE_TIME);
        if (null != obj) {
            Long ts = (Long) obj;
            setUpdateTime(new Date(ts));
        }

        if (isNeedResetExist) {
            setExist(true);
        }
    }

    @Override
    public ScmId getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    private void setName(String attrName) throws ScmException {
        checkStrNotEmpty(FieldName.Attribute.FIELD_NAME, attrName);
        this.name = attrName;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public void setDisplayName(String displayName) throws ScmException {
        checkArgNotNull(FieldName.Attribute.FIELD_DISPLAY_NAME, displayName);

        if (isExist()) {
            BSONObject attrInfo = new BasicBSONObject();
            attrInfo.put(FieldName.Attribute.FIELD_DISPLAY_NAME, displayName);
            updateAttrInfo(attrInfo);
        }

        this.displayName = displayName;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String desc) throws ScmException {
        checkArgNotNull(FieldName.Attribute.FIELD_DESCRIPTION, desc);
        if (isExist()) {
            BSONObject attrInfo = new BasicBSONObject();
            attrInfo.put(FieldName.Attribute.FIELD_DESCRIPTION, desc);
            updateAttrInfo(attrInfo);
        }
        this.description = desc;
    }

    @Override
    public AttributeType getType() {
        return this.type;
    }

    // delete, not support, if type need change, create new attribute.
    /*
     * @Override public void setTypeAndCheckRule(AttributeType type, ScmAttrRule rule) throws
     * ScmException { checkArgNotNull(FieldName.Attribute.FIELD_TYPE, type);
     * checkArgNotNull(FieldName.Attribute.FIELD_CHECK_RULE, rule);
     * 
     * if (isExist()) { BSONObject attrInfo = new BasicBSONObject();
     * attrInfo.put(FieldName.Attribute.FIELD_TYPE, type.getName());
     * attrInfo.put(FieldName.Attribute.FIELD_CHECK_RULE, rule.toBSONObject());
     * updateAttrInfo(attrInfo); } this.type = type; this.checkRule = rule; }
     */

    @Override
    public ScmAttrRule getCheckRule() {
        return this.checkRule;
    }

    @Override
    public void setCheckRule(ScmAttrRule rule) throws ScmException {
        if (type == null) {
            throw new ScmException(ScmError.OPERATION_UNSUPPORTED, FieldName.Attribute.FIELD_TYPE
                    + " undefined, 'setCheckRule' operation cannot be done");
        }

        if (isExist()) {
            BSONObject attrInfo = new BasicBSONObject();
            if (rule == null) {
                attrInfo.put(FieldName.Attribute.FIELD_CHECK_RULE, new BasicBSONObject());
            }
            else {
                attrInfo.put(FieldName.Attribute.FIELD_CHECK_RULE, rule.toBSONObject());
            }
            updateAttrInfo(attrInfo);
        }
        this.checkRule = rule;
    }

    @Override
    public boolean isRequired() {
        return this.required;
    }

    @Override
    public void setRequired(boolean required) throws ScmException {
        if (isExist()) {
            BSONObject attrInfo = new BasicBSONObject();
            attrInfo.put(FieldName.Attribute.FIELD_REQUIRED, required);
            updateAttrInfo(attrInfo);
        }
        this.required = required;
    }

    @Override
    public String getCreateUser() {
        return this.createUser;
    }

    private void setCreateUser(String user) throws ScmException {
        this.createUser = user;
    }

    @Override
    public Date getCreateTime() {
        return this.createTime;
    }

    private void setCreateTime(Date date) {
        this.createTime = date;
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
                    "attribute '" + field + "' cannot be null or an empty string");
        }
    }

    private void updateAttrInfo(BSONObject updateInfo) throws ScmException {
        BSONObject attrInfo = getSession().getDispatcher().updateAttributeInfo(this.ws.getName(),
                getId(), updateInfo);
        refresh(attrInfo);
    }

    @Override
    public void delete() throws ScmException {
        if (isExist()) {
            getSession().getDispatcher().deleteAttribute(this.ws.getName(), getId());
            setExist(false);
        }
    }

    /*
     * @Override public ScmId save() throws ScmException { if (isExist()) { throw new
     * ScmException(ScmError.OPERATION_UNSUPPORTED,
     * "attribute already exists, save operation cannot be done repeatedly"); }
     * 
     * BSONObject info = new BasicBSONObject();
     * 
     * checkStrNotEmpty(FieldName.Attribute.FIELD_NAME, getName());
     * info.put(FieldName.Attribute.FIELD_NAME, getName());
     * 
     * info.put(FieldName.Attribute.FIELD_DISPLAY_NAME, getDisplayName());
     * 
     * info.put(FieldName.Attribute.FIELD_DESCRIPTION, getDescription());
     * 
     * checkArgNotNull(FieldName.Attribute.FIELD_TYPE, getType());
     * info.put(FieldName.Attribute.FIELD_TYPE, getType().getName());
     * 
     * if (getCheckRule() != null) { info.put(FieldName.Attribute.FIELD_CHECK_RULE,
     * getCheckRule().toBSONObject()); }
     * 
     * info.put(FieldName.Attribute.FIELD_REQUIRED, isRequired());
     * 
     * BSONObject batchInfo = getSession().getDispatcher().createAttribute(this.ws.getName(), info);
     * 
     * refresh(batchInfo); setExist(true); return getId(); }
     */

    private void checkArgNotNull(String argName, Object arg) throws ScmException {
        if (arg == null) {
            throw new ScmException(ScmError.INVALID_ARGUMENT, argName + " cannot be null");
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        buf.append("id : \"" + this.id + "\", ");
        buf.append("name : \"" + this.name + "\", ");
        buf.append("displayName : \"" + this.displayName + "\", ");
        buf.append("description : \"" + this.description + "\", ");
        buf.append("type : \"" + this.type + "\", ");
        buf.append("checkRule : " + this.checkRule + ", ");
        buf.append("required : " + this.required);
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
