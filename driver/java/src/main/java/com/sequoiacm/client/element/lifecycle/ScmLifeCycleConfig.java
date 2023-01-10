package com.sequoiacm.client.element.lifecycle;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.List;

public class ScmLifeCycleConfig {

    private List<ScmLifeCycleStageTag> stageTagConfig;

    private List<ScmLifeCycleTransition> transitionConfig;

    public ScmLifeCycleConfig() {
    }

    public ScmLifeCycleConfig(BSONObject obj) throws ScmException {
        Object temp = null;

        BSONObject lifeCycleConfiguration = BsonUtils.getBSONObjectChecked(obj,
                "LifeCycleConfiguration");
        BSONObject transitionConfiguration = BsonUtils.getBSONObject(lifeCycleConfiguration,
                "TransitionConfiguration");
        BSONObject stageTagConfiguration = BsonUtils.getBSONObject(lifeCycleConfiguration,
                "StageTagConfiguration");

        if (null != transitionConfiguration) {
            temp = transitionConfiguration.get("Transition");
            if (null != temp) {
                transitionConfig = new ArrayList<ScmLifeCycleTransition>();
                if (temp instanceof BasicBSONObject) {
                    transitionConfig.add(new ScmLifeCycleTransition((BSONObject) temp));
                }
                else if (temp instanceof BasicBSONList) {
                    BasicBSONList l = (BasicBSONList) temp;
                    for (Object o : l) {
                        transitionConfig.add(new ScmLifeCycleTransition((BSONObject) o));
                    }
                }
                else {
                    throw new ScmException(ScmError.INVALID_ARGUMENT,
                            "can not analysis TransitionConfiguration");
                }
            }
        }

        if (null != stageTagConfiguration) {
            temp = stageTagConfiguration.get("StageTag");
            if (null != temp) {
                stageTagConfig = new ArrayList<ScmLifeCycleStageTag>();
                if (temp instanceof BasicBSONObject) {
                    stageTagConfig.add(new ScmLifeCycleStageTag((BSONObject) temp));
                }
                else if (temp instanceof BasicBSONList) {
                    BasicBSONList l = (BasicBSONList) temp;
                    for (Object o : l) {
                        stageTagConfig.add(new ScmLifeCycleStageTag((BSONObject) o));
                    }
                }
                else {
                    throw new ScmException(ScmError.INVALID_ARGUMENT,
                            "can not analysis StageTagConfiguration");
                }
            }
        }
    }

    public List<ScmLifeCycleStageTag> getStageTagConfig() {
        return stageTagConfig;
    }

    public void setStageTagConfig(List<ScmLifeCycleStageTag> stageTagConfig) {
        this.stageTagConfig = stageTagConfig;
    }

    public List<ScmLifeCycleTransition> getTransitionConfig() {
        return transitionConfig;
    }

    public void setTransitionConfig(List<ScmLifeCycleTransition> transitionConfig) {
        this.transitionConfig = transitionConfig;
    }

    public BSONObject toBSONObject() {
        BSONObject bsonObject = new BasicBSONObject();

        BasicBSONList scmStageTagConfig = new BasicBSONList();
        for (ScmLifeCycleStageTag stageTag : stageTagConfig) {
            scmStageTagConfig.add(stageTag.toBSONObject());
        }
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_CONFIG,
                scmStageTagConfig);

        BasicBSONList scmTransitionConfig = new BasicBSONList();
        for (ScmLifeCycleTransition transition : transitionConfig) {
            scmTransitionConfig.add(transition.toBSONObject());
        }
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_CONFIG,
                scmTransitionConfig);

        return bsonObject;
    }

    public ScmLifeCycleConfig fromBSONObject(BSONObject obj){
        stageTagConfig = new ArrayList<ScmLifeCycleStageTag>();
        transitionConfig = new ArrayList<ScmLifeCycleTransition>();
        Object temp = null;

        temp = obj.get(FieldName.LifeCycleConfig.FIELD_STAGE_TAG_CONFIG);
        if (null != temp) {
            BasicBSONList l = (BasicBSONList) temp;
            for (Object o : l) {
                stageTagConfig.add(new ScmLifeCycleStageTag().fromBSONObject((BSONObject) o));
            }
        }

        temp = obj.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_CONFIG);
        if (null != temp) {
            BasicBSONList l = (BasicBSONList) temp;
            for (Object o : l) {
                transitionConfig.add(new ScmLifeCycleTransition().fromBSONObject((BSONObject) o));
            }
        }
        return this;
    }
}
