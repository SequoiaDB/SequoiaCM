
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

public class ScmTransitionTriggers {
    private String mode;
    private long maxExecTime;
    private String rule;
    private List<ScmTrigger> triggerList;

    public ScmTransitionTriggers(){}

    public ScmTransitionTriggers(BSONObject content) throws ScmException {
        Object temp = null;
        temp = content.get("Mode");
        if (null != temp) {
            setMode((String) temp);
        }

        temp = content.get("MaxExecTime");
        if (null != temp) {
            setMaxExecTime((Integer) temp);
        }

        temp = content.get("Rule");
        if (null != temp) {
            setRule((String) temp);
        }

        temp = content.get("Trigger");
        if (null != temp) {
            triggerList = new ArrayList<ScmTrigger>();
            if (temp instanceof BasicBSONObject) {
                triggerList.add(new ScmTrigger((BSONObject) temp));
            }
            else if (temp instanceof BasicBSONList){
                BasicBSONList l = (BasicBSONList) temp;
                for (Object o : l) {
                    triggerList.add(new ScmTrigger((BSONObject) o));
                }
            }
            else {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "can not analysis TransitionTrigger's Trigger");
            }
        }
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

    public static class Builder {
        private ScmTransitionTriggers transitionTriggers;
        private List<ScmTrigger> triggerList;

        public Builder(String mode, long maxExecTime, String rule) throws ScmException {
            checkStringArgNotEmpty("transition trigger mode", mode);
            checkStringArgNotEmpty("transition trigger rule", rule);
            if (maxExecTime < 0) {
                throw new ScmInvalidArgumentException(
                        "invalid transition maxExecTime=" + maxExecTime);
            }
            transitionTriggers = new ScmTransitionTriggers();
            transitionTriggers.setMode(mode);
            transitionTriggers.setMaxExecTime(maxExecTime);
            transitionTriggers.setRule(rule);
            triggerList = new ArrayList<ScmTrigger>();
        }

        public Builder addTrigger(String id, String mode, String createTime, String lastAccessTime,
                String buildTime) throws ScmException {
            checkStringArgNotEmpty("trigger ID", mode);
            checkStringArgNotEmpty("trigger mode", mode);
            triggerList.add(new ScmTrigger(id, mode, createTime, lastAccessTime, buildTime));
            return this;
        }

        public ScmTransitionTriggers build() {
            transitionTriggers.setTriggerList(triggerList);
            return transitionTriggers;
        }
    }

    public BSONObject toBSONObject(){
        BSONObject bsonObject = new BasicBSONObject();
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGERS_MODE, mode);
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGERS_RULE, rule);
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGERS_MAX_EXEC_TIME,
                maxExecTime);
        List<BSONObject> triggerListBSON = new ArrayList<BSONObject>();
        for (ScmTrigger trigger : triggerList) {
            triggerListBSON.add(trigger.toBSONObject("TransitionTriggers"));
        }

        bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGERS_TRIGGER_LIST,
                triggerListBSON);
        return bsonObject;
    }

    public ScmTransitionTriggers formBSONObject(BSONObject content){
        Object temp = null;
        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGERS_MODE);
        if (null != temp) {
            setMode((String) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGERS_MAX_EXEC_TIME);
        if (null != temp) {
            setMaxExecTime((Integer) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGERS_RULE);
        if (null != temp) {
            setRule((String) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGERS_TRIGGER_LIST);
        if (null != temp) {
            BasicBSONList l = (BasicBSONList) temp;
            List<ScmTrigger> list = new ArrayList<ScmTrigger>();
            for (Object o : l) {
                list.add(new ScmTrigger().fromBSONObject((BSONObject) o));
            }
            setTriggerList(list);
        }
        return this;
    }

    private static void checkStringArgNotEmpty(String argName, String argValue)
            throws ScmException {
        if (!Strings.hasText(argValue)) {
            throw new ScmInvalidArgumentException(argName + " is null or empty");
        }
    }
}
