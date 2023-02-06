package com.sequoiacm.client.element.lifecycle;

import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

/**
 * The trigger info.
 */
public class ScmTrigger {
    private String id;
    private String mode;
    private String createTime;
    private String lastAccessTime;
    private String buildTime;
    private String transitionTime;

    public ScmTrigger() {
    }

    public static ScmTrigger fromUser(BSONObject content) {
        ScmTrigger trigger = new ScmTrigger();
        Object temp = null;
        temp = content.get("ID");
        if (null != temp) {
            trigger.id = (String.valueOf(temp));
        }

        temp = content.get("Mode");
        if (null != temp) {
            trigger.mode = ((String) temp);
        }

        temp = content.get("CreateTime");
        if (null != temp) {
            trigger.createTime = ((String) temp);
        }

        temp = content.get("LastAccessTime");
        if (null != temp) {
            trigger.lastAccessTime = ((String) temp);
        }

        temp = content.get("BuildTime");
        if (null != temp) {
            trigger.buildTime = ((String) temp);
        }

        temp = content.get("TransitionTime");
        if (null != temp) {
            trigger.transitionTime = ((String) temp);
        }
        return trigger;
    }

    public ScmTrigger(String id, String mode, String createTime, String lastAccessTime,
            String buildTime) {
        this.id = id;
        this.mode = mode;
        this.createTime = createTime;
        this.lastAccessTime = lastAccessTime;
        this.buildTime = buildTime;
    }

    public ScmTrigger(String id, String mode, String lastAccessTime, String transitionTime) {
        this.id = id;
        this.mode = mode;
        this.lastAccessTime = lastAccessTime;
        this.transitionTime = transitionTime;
    }

    /**
     * Return the trigger id.
     * 
     * @return the trigger id.
     */
    public String getID() {
        return id;
    }

    /**
     * Set the trigger id.
     * 
     * @param id
     *            trigger id.
     */
    public void setID(String id) {
        this.id = id;
    }

    /**
     * Return the trigger mode.
     * 
     * @return the trigger mode.
     */
    public String getMode() {
        return mode;
    }

    /**
     * Set the trigger mode, can only set 'ALL' or 'ANY'.
     * 
     * @param mode
     *            trigger mode.
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Return the trigger createTime.
     * 
     * @return the trigger createTime.
     */
    public String getCreateTime() {
        return createTime;
    }

    /**
     * Set the trigger createTime.
     * 
     * @param createTime
     *            trigger createTime
     */
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    /**
     * Return the trigger lastAccessTime.
     * 
     * @return the trigger lastAccessTime.
     */
    public String getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * Set the trigger lastAccessTime.
     * 
     * @param lastAccessTime
     *            trigger lastAccessTime.
     */
    public void setLastAccessTime(String lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    /**
     * Return the trigger buildTime.
     * 
     * @return the trigger buildTime.
     */
    public String getBuildTime() {
        return buildTime;
    }

    /**
     * Set the trigger buildTime.
     * 
     * @param buildTime
     *            trigger buildTime.
     */
    public void setBuildTime(String buildTime) {
        this.buildTime = buildTime;
    }

    /**
     * Return the trigger transitionTime.
     * 
     * @return trigger transitionTime.
     */
    public String getTransitionTime() {
        return transitionTime;
    }

    /**
     * Set the trigger transitionTime.
     * 
     * @param transitionTime
     *            trigger transitionTime.
     */
    public void setTransitionTime(String transitionTime) {
        this.transitionTime = transitionTime;
    }

    public BSONObject toBSONObject(String type) {
        BSONObject bsonObject = new BasicBSONObject();
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_ID, id);
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_MODE, mode);
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_LAST_ACCESS_TIME,
                lastAccessTime);
        if (type.equals("TransitionTriggers")) {
            bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_CREATE_TIME,
                    createTime);
            bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_BUILD_TIME,
                    buildTime);
        }
        else if (type.equals("CleanTriggers")) {
            bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_TRANSITION_TIME,
                    transitionTime);
        }
        return bsonObject;
    }

    public static ScmTrigger fromRecord(BSONObject content) {
        ScmTrigger trigger = new ScmTrigger();
        Object temp = null;
        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_ID);
        if (null != temp) {
            trigger.id = ((String) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_MODE);
        if (null != temp) {
            trigger.mode = ((String) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_CREATE_TIME);
        if (null != temp) {
            trigger.createTime = ((String) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_LAST_ACCESS_TIME);
        if (null != temp) {
            trigger.lastAccessTime = ((String) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_BUILD_TIME);
        if (null != temp) {
            trigger.buildTime = ((String) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_TRANSITION_TIME);
        if (null != temp) {
            trigger.transitionTime = ((String) temp);
        }

        return trigger;
    }
}
