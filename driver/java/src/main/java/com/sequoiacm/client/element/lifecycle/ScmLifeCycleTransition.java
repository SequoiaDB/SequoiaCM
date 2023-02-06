package com.sequoiacm.client.element.lifecycle;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;

/**
 * The transition info.
 */
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

    /**
     * Return the transition name.
     * 
     * @return the transition name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the transition name.
     * 
     * @param name
     *            the transition name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the transitionTriggers info.
     * 
     * @return ScmTransitionTriggers.
     */
    public ScmTransitionTriggers getTransitionTriggers() {
        return transitionTriggers;
    }

    /**
     * Set the transitionTriggers info.
     * 
     * @param transitionTriggers
     *            ScmTransitionTriggers.
     */
    public void setTransitionTriggers(ScmTransitionTriggers transitionTriggers) {
        this.transitionTriggers = transitionTriggers;
    }

    /**
     * Return the cleanTriggers info.
     * 
     * @return ScmCleanTriggers.
     */
    public ScmCleanTriggers getCleanTriggers() {
        return cleanTriggers;
    }

    /**
     * Set the cleanTriggers info.
     * 
     * @param cleanTriggers
     *            ScmCleanTriggers.
     */
    public void setCleanTriggers(ScmCleanTriggers cleanTriggers) {
        this.cleanTriggers = cleanTriggers;
    }

    /**
     * Return the transition matcher info for filter files.
     * 
     * @return the transition matcher info.
     */
    public String getMatcher() {
        return matcher;
    }

    /**
     * Set the transition matcher info for filter files.
     * 
     * @param matcher
     *            the transition matcher info.
     */
    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    /**
     * Return the flow's source info.
     * 
     * @return the flow's source info.
     */
    public String getSource() {
        return source;
    }

    /**
     * Set the flow's source info.
     * 
     * @param source
     *            the flow's source info.
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Return the flow's dest info.
     *
     * @return the flow's dest info.
     */
    public String getDest() {
        return dest;
    }

    /**
     * Set the flow's dest info.
     * 
     * @param dest
     *            the flow's dest info.
     */
    public void setDest(String dest) {
        this.dest = dest;
    }

    /**
     * Is need quick start.
     * 
     * @return Is need quick start.
     */
    public boolean isQuickStart() {
        return isQuickStart;
    }

    /**
     * Set need quick start.
     * 
     * @param quickStart
     *            Is need quick start.
     */
    public void setQuickStart(boolean quickStart) {
        isQuickStart = quickStart;
    }

    /**
     * Is need recycle space.
     * 
     * @return Is need recycle space.
     */
    public boolean isRecycleSpace() {
        return isRecycleSpace;
    }

    /**
     * Set need recycle space.
     * 
     * @param recycleSpace
     *            Is need recycle space.
     */
    public void setRecycleSpace(boolean recycleSpace) {
        isRecycleSpace = recycleSpace;
    }

    /**
     * Return the data check level for files.
     * 
     * @return the data check level.
     */
    public String getDataCheckLevel() {
        return dataCheckLevel;
    }

    /**
     * Set the data check level for files, can only set 'strict' or 'week'.
     * 
     * @param dataCheckLevel
     *            the data check level.
     */
    public void setDataCheckLevel(String dataCheckLevel) {
        this.dataCheckLevel = dataCheckLevel;
    }

    /**
     * Return the scope of the task.
     * 
     * @return the scope.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Set the scope of the task, can only set 'ALL' or 'CURRENT' or 'HISTORY'.
     * 
     * @param scope
     *            the scope.
     */
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
