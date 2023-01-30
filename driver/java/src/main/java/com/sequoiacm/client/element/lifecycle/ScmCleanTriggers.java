package com.sequoiacm.client.element.lifecycle;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.Strings;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.List;

public class ScmCleanTriggers {
    private String mode;

    private long maxExecTime;

    private String rule;

    private List<ScmTrigger> triggerList;

    public ScmCleanTriggers(){}

    public static ScmCleanTriggers fromUser(BSONObject content) throws ScmException {
        ScmCleanTriggers triggers = new ScmCleanTriggers();
        Object temp = null;
        temp = content.get("Mode");
        if (null != temp) {
            triggers.mode = ((String) temp);
        }

        temp = content.get("MaxExecTime");
        if (null != temp) {
            triggers.maxExecTime = ((Integer) temp);
        }

        temp = content.get("Rule");
        if (null != temp) {
            triggers.rule = ((String) temp);
        }

        temp = content.get("Trigger");
        if (null != temp) {
            List<ScmTrigger> triggerList = new ArrayList<ScmTrigger>();
            if (temp instanceof BasicBSONObject) {
                triggerList.add(ScmTrigger.fromUser((BSONObject) temp));
            }
            else if (temp instanceof BasicBSONList) {
                BasicBSONList l = (BasicBSONList) temp;
                for (Object o : l) {
                    triggerList.add(ScmTrigger.fromUser((BSONObject) o));
                }
            }
            else {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "can not analysis CleanTrigger's Trigger");
            }
            triggers.triggerList = triggerList;
        }

        return triggers;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public long getMaxExecTime() {
        return maxExecTime;
    }

    public void setMaxExecTime(long maxExecTime) {
        this.maxExecTime = maxExecTime;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public List<ScmTrigger> getTriggerList() {
        return triggerList;
    }

    public void setTriggerList(List<ScmTrigger> triggerList) {
        this.triggerList = triggerList;
    }

    public BSONObject toBSONObject(){
        BSONObject bsonObject = new BasicBSONObject();
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_MODE, mode);
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_RULE, rule);
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_MAX_EXEC_TIME, maxExecTime);
        List<BSONObject> triggerListBSON = new ArrayList<BSONObject>();
        for (ScmTrigger trigger : triggerList) {
            triggerListBSON.add(trigger.toBSONObject("CleanTriggers"));
        }
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_TRIGGER_LIST,
                triggerListBSON);
        return bsonObject;
    }

    public static class Builder {
        private ScmCleanTriggers cleanTriggers;
        private List<ScmTrigger> triggerList;

        public Builder(String mode, long maxExecTime, String rule) throws ScmException {
            checkStringArgNotEmpty("clean trigger mode", mode);
            checkStringArgNotEmpty("clean trigger rule", rule);
            if (maxExecTime < 0) {
                throw new ScmInvalidArgumentException("invalid clean maxExecTime=" + maxExecTime);
            }
            cleanTriggers = new ScmCleanTriggers();
            cleanTriggers.setMode(mode);
            cleanTriggers.setMaxExecTime(maxExecTime);
            cleanTriggers.setRule(rule);
            triggerList = new ArrayList<ScmTrigger>();
        }

        public Builder addTrigger(String ID, String mode, String transitionTime,
                String lastAccessTime) throws ScmException {
            checkStringArgNotEmpty("trigger ID", mode);
            checkStringArgNotEmpty("trigger mode", mode);
            triggerList.add(new ScmTrigger(ID, mode, lastAccessTime, transitionTime));
            return this;
        }

        public ScmCleanTriggers build() {
            cleanTriggers.setTriggerList(triggerList);
            return cleanTriggers;
        }
    }

    public static ScmCleanTriggers fromRecord(BSONObject content) {
        ScmCleanTriggers triggers = new ScmCleanTriggers();

        Object temp = null;
        temp = content.get(FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_MODE);
        if (null != temp) {
            triggers.mode = ((String) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_MAX_EXEC_TIME);
        if (null != temp) {
            triggers.maxExecTime = ((Integer) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_RULE);
        if (null != temp) {
            triggers.rule = ((String) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_TRIGGER_LIST);
        if (null != temp) {
            BasicBSONList l = (BasicBSONList) temp;
            List<ScmTrigger> list = new ArrayList<ScmTrigger>();
            for (Object o : l) {
                list.add(ScmTrigger.fromRecord((BSONObject) o));
            }
            triggers.triggerList = list;
        }

        return triggers;
    }

    private static void checkStringArgNotEmpty(String argName, String argValue)
            throws ScmException {
        if (!Strings.hasText(argValue)) {
            throw new ScmInvalidArgumentException(argName + " is null or empty");
        }
    }

    public boolean isEmpty() {
        return mode == null && rule == null && triggerList == null && maxExecTime == 0;
    }
}
