package com.sequoiacm.client.element.lifecycle;

import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

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
    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(String lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public String getBuildTime() {
        return buildTime;
    }

    public void setBuildTime(String buildTime) {
        this.buildTime = buildTime;
    }

    public String getTransitionTime() {
        return transitionTime;
    }

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
