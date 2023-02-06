
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

/**
 * The transitionTriggers info.
 */
public class ScmTransitionTriggers {
    private String mode;
    private long maxExecTime;
    private String rule;
    private List<ScmTrigger> triggerList;

    public ScmTransitionTriggers(){}

    public static ScmTransitionTriggers fromUser(BSONObject content) throws ScmException {
        ScmTransitionTriggers triggers = new ScmTransitionTriggers();
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
            else if (temp instanceof BasicBSONList){
                BasicBSONList l = (BasicBSONList) temp;
                for (Object o : l) {
                    triggerList.add(ScmTrigger.fromUser((BSONObject) o));
                }
            }
            else {
                throw new ScmException(ScmError.INVALID_ARGUMENT,
                        "can not analysis TransitionTrigger's Trigger");
            }
            triggers.triggerList = triggerList;
        }

        return triggers;
    }

    /**
     * Return the transition triggers mode.
     *
     * @return The transition triggers mode.
     */
    public String getMode() {
        return mode;
    }

    /**
     * Set the transition triggers mode, can only set 'ALL' or 'ANY'.
     * 
     * @param mode
     *            transition triggers mode.
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Return the transition triggers maxExecTime.
     * 
     * @return the transition triggers maxExecTime.
     */
    public long getMaxExecTime() {
        return maxExecTime;
    }

    /**
     * Set the transition triggers maxExecTime.
     * 
     * @param maxExecTime
     *            transition triggers maxExecTime.
     */
    public void setMaxExecTime(long maxExecTime) {
        this.maxExecTime = maxExecTime;
    }

    /**
     * Return the transition triggers rule.
     * 
     * @return the transition triggers rule.
     */
    public String getRule() {
        return rule;
    }

    /**
     * Set the transition triggers rule, can only set a cron expression.
     * 
     * @param rule
     *            transition triggers rule.
     */
    public void setRule(String rule) {
        this.rule = rule;
    }

    /**
     * Return the ScmTrigger List.
     * 
     * @return the ScmTrigger List.
     */
    public List<ScmTrigger> getTriggerList() {
        return triggerList;
    }

    /**
     * Set the ScmTrigger List.
     * 
     * @param triggerList
     *            ScmTrigger List.
     */
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

    public static ScmTransitionTriggers formRecord(BSONObject content) {
        ScmTransitionTriggers triggers = new ScmTransitionTriggers();
        Object temp = null;
        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGERS_MODE);
        if (null != temp) {
            triggers.mode = ((String) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGERS_MAX_EXEC_TIME);
        if (null != temp) {
            triggers.maxExecTime = ((Integer) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGERS_RULE);
        if (null != temp) {
            triggers.rule = ((String) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGERS_TRIGGER_LIST);
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
}
