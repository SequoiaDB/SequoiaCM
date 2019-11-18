package com.sequoiacm.client.core;

import org.bson.BSONObject;
import org.bson.types.BSONTimestamp;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.FieldName;

/**
 * The information of audit.
 */
public class ScmAuditInfo {

    private String hostName;
    private String port;
    private String type;
    private String userType;
    private String userName;
    private String workspaceName;
    private String flag;
    private String time;
    private String thread;
    private String level;
    private String message;

    ScmAuditInfo() {

    }

    ScmAuditInfo(BSONObject bson) throws ScmException {
        hostName = (String) bson.get(FieldName.Audit.HOST);
        port = (String) bson.get(FieldName.Audit.PORT);
        type = (String) bson.get(FieldName.Audit.TYPE);
        userType = (String) bson.get(FieldName.Audit.USER_TYPE);
        userName = (String) bson.get(FieldName.Audit.USER_NAME);
        workspaceName = (String) bson.get(FieldName.Audit.WORK_SPACE);
        flag = (String) bson.get(FieldName.Audit.FLAG);
        time = ((BSONTimestamp) bson.get(FieldName.Audit.TIME)).toTimestamp().toString();
        thread = (String) bson.get(FieldName.Audit.THREAD);
        level = (String) bson.get(FieldName.Audit.LEVEL);
        message = (String) bson.get(FieldName.Audit.MESSAGE);
    }

    /**
     * Gets the host name of the audit.
     *
     * @return host name.
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Sets the host name of the audit.
     *
     * @param hostName
     *            host name.
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Gets the port of the audit.
     *
     * @return port.
     */
    public String getPort() {
        return port;
    }

    /**
     * Sets the port of the audit.
     *
     * @param port
     *            port.
     */
    public void setPort(String port) {
        this.port = port;
    }

    /**
     * Gets the type of the audit.
     *
     * @return type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the audit.
     *
     * @param type
     *            type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the user type of the audit.
     *
     * @return user type.
     */
    public String getUserType() {
        return userType;
    }

    /**
     * Sets the user type of the audit.
     *
     * @param userType
     *            user type.
     */
    public void setUserType(String userType) {
        this.userType = userType;
    }

    /**
     * Gets the user name of the audit.
     *
     * @return user name.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the user name of the audit.
     *
     * @param userName
     *            user name.
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Gets the workspace name of the audit.
     *
     * @return workspace name.
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * Sets the workspace name of the audit.
     *
     * @param workspaceName
     *            workspace name.
     */
    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    /**
     * Gets the flag of the audit.
     *
     * @return flag.
     */
    public String getFlag() {
        return flag;
    }

    /**
     * Sets the flag of the audit.
     *
     * @param flag
     *            flag.
     */
    public void setFlag(String flag) {
        this.flag = flag;
    }

    /**
     * Gets the time of the audit.
     *
     * @return time.
     */
    public String getTime() {
        return time;
    }

    /**
     * Sets the time of the audit.
     *
     * @param time
     *            time
     */
    public void setTime(String time) {
        this.time = time;
    }

    /**
     * Get the thread id of the audit.
     *
     * @return thread id.
     */
    public String getThread() {
        return thread;
    }

    /**
     * Sets the thread id of the audit.
     *
     * @param thread
     *            thread id.
     */
    public void setThread(String thread) {
        this.thread = thread;
    }

    /**
     * Gets the level of the audit.
     *
     * @return level.
     */
    public String getLevel() {
        return level;
    }

    /**
     * Sets the level of the audit.
     *
     * @param level
     *            level.
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * Gets the message of the audit.
     *
     * @return message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message of the audit.
     *
     * @param message
     *            message.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        buf.append("hostName : \"" + hostName + "\" , ");
        buf.append("port : \"" + port + "\" , ");
        buf.append("type : \"" + type + "\" , ");
        buf.append("userType : \"" + userType + "\" , ");
        buf.append("userName : \"" + userName + "\" , ");
        buf.append("workspaceName : \"" + workspaceName + "\" , ");
        buf.append("flag : \"" + flag + "\" , ");
        buf.append("time : \"" + time + "\" , ");
        buf.append("thread : \"" + thread + "\" , ");
        buf.append("level : \"" + level + "\" , ");
        buf.append("message : " + message);
        buf.append("}");
        return buf.toString();
    }
}
