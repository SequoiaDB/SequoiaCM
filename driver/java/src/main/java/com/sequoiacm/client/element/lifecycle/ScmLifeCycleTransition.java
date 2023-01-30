package com.sequoiacm.client.element.lifecycle;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;

public class ScmLifeCycleTransition {
    private String name;

    private String source;

    private String dest;

    private ScmTransitionTriggers transitionTriggers;

    private ScmCleanTriggers cleanTriggers;

    private String matcher;

    private boolean isQuickStart = false;

    private boolean isRecycleSpace = true;

    private String dataCheckLevel = "strict";

    private String scope = "ALL";

    public ScmLifeCycleTransition() {
        cleanTriggers = new ScmCleanTriggers();
    }

    public ScmLifeCycleTransition(String transitionName, String source, String dest,
            ScmTransitionTriggers transitionTriggers) {
        this.name = transitionName;
        this.source = source;
        this.dest = dest;
        this.transitionTriggers = transitionTriggers;
        cleanTriggers = new ScmCleanTriggers();
    }

    public static ScmLifeCycleTransition fromUser(BSONObject content) throws ScmException {
        ScmLifeCycleTransition transition = new ScmLifeCycleTransition();
        Object temp = null;
        temp = content.get("Name");
        if (null != temp) {
            transition.name = ((String) temp);
        }

        temp = content.get("Flow");
        if (null != temp) {
            BSONObject obj = (BSONObject) temp;
            transition.source = ((String) obj.get("Source"));
            transition.dest = ((String) obj.get("Dest"));
        }

        temp = content.get("Matcher");
        if (null != temp) {
            transition.matcher = ((String) temp);
        }

        temp = content.get("ExtraContent");
        if (null != temp) {
            BSONObject obj = (BSONObject) temp;
            transition.isQuickStart = (BsonUtils.getBoolean(obj, "QuickStart"));
            transition.scope = (BsonUtils.getString(obj, "Scope"));
            transition.isRecycleSpace = (BsonUtils.getBoolean(obj, "RecycleSpace"));
            transition.dataCheckLevel = (BsonUtils.getString(obj, "DataCheckLevel"));
        }

        temp = content.get("TransitionTriggers");
        if (null != temp) {
            transition.transitionTriggers = (ScmTransitionTriggers.fromUser((BSONObject) temp));
        }

        temp = content.get("CleanTriggers");
        if (null != temp) {
            transition.cleanTriggers = (ScmCleanTriggers.fromUser((BSONObject) temp));
        }
        return transition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ScmTransitionTriggers getTransitionTriggers() {
        return transitionTriggers;
    }

    public void setTransitionTriggers(ScmTransitionTriggers transitionTriggers) {
        this.transitionTriggers = transitionTriggers;
    }

    public ScmCleanTriggers getCleanTriggers() {
        return cleanTriggers;
    }

    public void setCleanTriggers(ScmCleanTriggers cleanTriggers) {
        this.cleanTriggers = cleanTriggers;
    }

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public boolean isQuickStart() {
        return isQuickStart;
    }

    public void setQuickStart(boolean quickStart) {
        isQuickStart = quickStart;
    }

    public boolean isRecycleSpace() {
        return isRecycleSpace;
    }

    public void setRecycleSpace(boolean recycleSpace) {
        isRecycleSpace = recycleSpace;
    }

    public String getDataCheckLevel() {
        return dataCheckLevel;
    }

    public void setDataCheckLevel(String dataCheckLevel) {
        this.dataCheckLevel = dataCheckLevel;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public BSONObject toBSONObject() {
        BSONObject bsonObject = new BasicBSONObject();
        if (name != null) {
            bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME, name);
        }
        if (matcher != null) {
            BSONObject matcherBSON = (BSONObject) JSON.parse(matcher);
            bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_MATCHER, matcherBSON);
        }
        if (source != null && dest != null) {
            BSONObject flow = new BasicBSONObject();
            flow.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW_SOURCE, source);
            flow.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW_DEST, dest);
            bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW, flow);
        }
        if (transitionTriggers != null) {
            bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRANSITION_TRIGGERS,
                    transitionTriggers.toBSONObject());
        }
        if (cleanTriggers != null && !cleanTriggers.isEmpty()) {
            bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_CLEAN_TRIGGERS,
                    cleanTriggers.toBSONObject());
        }

        BSONObject extraContent = new BasicBSONObject();
        extraContent.put(FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_QUICK_START, isQuickStart);
        extraContent.put(FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_SCOPE, scope);
        extraContent.put(FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_DATA_CHECK_LEVEL,
                dataCheckLevel);
        extraContent.put(FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_RECYCLE_SPACE,
                isRecycleSpace);
        bsonObject.put(FieldName.LifeCycleConfig.FIELD_TRANSITION_EXTRA_CONTENT, extraContent);

        return bsonObject;
    }

    public static ScmLifeCycleTransition fromRecord(BSONObject content) {
        ScmLifeCycleTransition transition = new ScmLifeCycleTransition();
        Object temp = null;
        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_NAME);
        if (null != temp) {
            transition.name = ((String) temp);
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW);
        if (null != temp) {
            BSONObject flow = (BSONObject) temp;
            transition.source = ((String) flow
                    .get(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW_SOURCE));
            transition.dest = ((String) flow
                    .get(FieldName.LifeCycleConfig.FIELD_TRANSITION_FLOW_DEST));
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_MATCHER);
        if (null != temp) {
            BSONObject o = (BSONObject) temp;
            transition.matcher = (o.toString());
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_EXTRA_CONTENT);
        if (null != temp) {
            BSONObject extraContent = (BSONObject) temp;
            transition.scope = ((String) extraContent
                    .get(FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_SCOPE));
            transition.dataCheckLevel = ((String) extraContent
                    .get(FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_DATA_CHECK_LEVEL));
            transition.isQuickStart = ((Boolean) extraContent
                    .get(FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_QUICK_START));
            transition.isRecycleSpace = ((Boolean) extraContent
                    .get(FieldName.LifeCycleConfig.FIELD_EXTRA_CONTENT_RECYCLE_SPACE));
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_TRANSITION_TRIGGERS);
        if (null != temp) {
            transition.transitionTriggers = (ScmTransitionTriggers.formRecord((BSONObject) temp));
        }

        temp = content.get(FieldName.LifeCycleConfig.FIELD_TRANSITION_CLEAN_TRIGGERS);
        if (null != temp) {
            transition.cleanTriggers = (ScmCleanTriggers.fromRecord((BSONObject) temp));
        }

        return transition;
    }
}
