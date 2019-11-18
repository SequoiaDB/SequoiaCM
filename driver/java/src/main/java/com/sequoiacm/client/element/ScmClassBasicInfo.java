package com.sequoiacm.client.element;

import java.util.Date;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmSystemException;
import com.sequoiacm.common.FieldName;

/**
 * The information of ScmClass.
 */
public class ScmClassBasicInfo {
    private ScmId id;
    private String name;
    private String description = "";
    private String createUser;
    private Date createTime;

    /**
     * Create class basic info.
     */
    public ScmClassBasicInfo() {
    }

    /**
     * Create class basic info.
     *
     * @param classBasicInfo
     *            a bson containing basic information about scm class.
     * @throws ScmException
     *             if error happens.
     */
    public ScmClassBasicInfo(BSONObject classBasicInfo) throws ScmException {
        try {
            Object obj = null;
            obj = getValueCheckNotNull(classBasicInfo, FieldName.Class.FIELD_ID);
            this.id = new ScmId(String.valueOf(obj), false);

            obj = getValueCheckNotNull(classBasicInfo, FieldName.Class.FIELD_NAME);
            this.name = String.valueOf(obj);

            obj = getValueCheckNotNull(classBasicInfo, FieldName.Class.FIELD_DESCRIPTION);
            this.description = String.valueOf(obj);

            obj = getValueCheckNotNull(classBasicInfo, FieldName.Class.FIELD_INNER_CREATE_USER);
            this.createUser = String.valueOf(obj);

            obj = getValueCheckNotNull(classBasicInfo, FieldName.Class.FIELD_INNER_CREATE_TIME);
            this.createTime = new Date((Long) obj);
        }
        catch (ScmException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ScmSystemException(
                    "failed to create ScmClassBasicInfo:record=" + classBasicInfo, e);
        }
    }

    /**
     * Gets the id of the scm class.
     *
     * @return scm class id.
     */
    public ScmId getId() {
        return id;
    }

    /**
     * Sets the id of the scm class.
     *
     * @param id
     *            scm class id.
     */
    public void setId(ScmId id) {
        this.id = id;
    }

    /**
     * Gets the description of the scm class.
     *
     * @return scm class description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the scm class.
     *
     * @param description
     *            description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the created user of the scm class.
     *
     * @return created user.
     */
    public String getCreateUser() {
        return createUser;
    }

    /**
     * Sets the created user of the scm class.
     *
     * @param createUser
     *            created user.
     */
    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    /**
     * Gets the created time of the scm class.
     *
     * @return created time.
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * Sets the created time of the scm class.
     *
     * @param createTime
     *            created time.
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * Gets the name of the scm class.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the scm class.
     *
     * @param name
     *            name of scm class.
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        buf.append("id : \"" + id + "\", ");
        buf.append("name : \"" + name + "\", ");
        buf.append("description : \"" + description + "\", ");
        buf.append("createUser : \"" + createUser + "\", ");
        buf.append("createTime : " + createTime.getTime() + "");
        buf.append("}");
        return buf.toString();
    }

    private Object getValueCheckNotNull(BSONObject classInfo, String key) throws ScmException {
        Object value = classInfo.get(key);
        if (value == null) {
            throw new ScmSystemException(
                    "ClassBasicInfo missing key:record=" + classInfo + ",key=" + key);
        }
        return value;
    }
}
