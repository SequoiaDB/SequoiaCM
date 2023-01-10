package com.sequoiacm.schedule.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.schedule.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.List;

public class ScmCleanTriggers {
    private String mode;
    @JsonProperty("max_exec_time")
    private long maxExecTime;
    private String rule;
    @JsonProperty("trigger_list")
    private List<Trigger> triggerList;

    public ScmCleanTriggers(BSONObject obj) {
        this.mode = BsonUtils.getStringChecked(obj,
                FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_MODE);
        this.maxExecTime = BsonUtils
                .getNumberChecked(obj, FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_MAX_EXEC_TIME)
                .longValue();

        this.rule = BsonUtils.getStringChecked(obj,
                FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_RULE);

        triggerList = new ArrayList<>();
        BasicBSONList array = BsonUtils.getArrayChecked(obj,
                FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_TRIGGER_LIST);
        for (Object o : array) {
            triggerList.add(new Trigger((BSONObject) o));
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

    public List<Trigger> getTriggerList() {
        return triggerList;
    }

    public void setTriggerList(List<Trigger> triggerList) {
        this.triggerList = triggerList;
    }

    public BSONObject toBSONObj() {
        BSONObject obj = new BasicBSONObject();
        obj.put(FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_MODE, mode);
        obj.put(FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_MAX_EXEC_TIME, maxExecTime);
        obj.put(FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_RULE, rule);

        BasicBSONList l = new BasicBSONList();
        for (Trigger trigger : triggerList) {
            l.add(trigger.toBSONObject());
        }
        obj.put(FieldName.LifeCycleConfig.FIELD_CLEAN_TRIGGERS_TRIGGER_LIST, l);
        return obj;
    }

    public static class Trigger {
        private String id;
        private String mode;
        @JsonProperty("transition_time")
        private String transitionTime;
        @JsonProperty("last_access_time")
        private String lastAccessTime;

        public Trigger(BSONObject obj) {
            this.id = BsonUtils.getStringChecked(obj,
                    FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_ID);
            this.mode = BsonUtils.getStringChecked(obj,
                    FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_MODE);
            this.transitionTime = BsonUtils.getStringChecked(obj,
                    FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_TRANSITION_TIME);
            this.lastAccessTime = BsonUtils.getStringChecked(obj,
                    FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_LAST_ACCESS_TIME);
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getTransitionTime() {
            return transitionTime;
        }

        public void setTransitionTime(String transitionTime) {
            this.transitionTime = transitionTime;
        }

        public String getLastAccessTime() {
            return lastAccessTime;
        }

        public void setLastAccessTime(String lastAccessTime) {
            this.lastAccessTime = lastAccessTime;
        }

        public BSONObject toBSONObject() {
            BSONObject obj = new BasicBSONObject();
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_ID, id);
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_MODE, mode);
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_TRANSITION_TIME, transitionTime);
            obj.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRIGGER_LAST_ACCESS_TIME,
                    lastAccessTime);
            return obj;
        }
    }
}
