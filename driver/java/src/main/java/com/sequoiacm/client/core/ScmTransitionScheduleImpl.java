package com.sequoiacm.client.core;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import java.util.ArrayList;
import java.util.List;

public class ScmTransitionScheduleImpl implements ScmTransitionSchedule {
    private ScmSession session;
    private String id;
    private String workspace;
    private String createUser;
    private String updateUser;
    private long createTime;
    private long updateTime;
    private boolean customized;
    private boolean enable;
    private String preferredRegion;
    private String preferredZone;
    private List<ScmId> scheduleIds;
    private ScmLifeCycleTransition transition;

    ScmTransitionScheduleImpl(ScmSession ss, BSONObject obj) throws ScmException {
        this.session = ss;
        fromBSONObject(obj);
    }

    private void fromBSONObject(BSONObject obj) throws ScmException {
        id = BsonUtils.getStringChecked(obj,
                FieldName.LifeCycleConfig.FIELD_TRANSITION_SCHEDULE_ID);
        workspace = BsonUtils.getStringChecked(obj,
                FieldName.LifeCycleConfig.FIELD_TRANSITION_SCHEDULE_WORKSPACE);
        createUser = BsonUtils.getStringChecked(obj,
                FieldName.LifeCycleConfig.FIELD_TRANSITION_SCHEDULE_CREATE_USER);
        updateUser = BsonUtils.getStringChecked(obj,
                FieldName.LifeCycleConfig.FIELD_TRANSITION_SCHEDULE_UPDATE_USER);
        createTime = BsonUtils.getLongChecked(obj,
                FieldName.LifeCycleConfig.FIELD_TRANSITION_SCHEDULE_CREATE_TIME);
        updateTime = BsonUtils.getLongChecked(obj,
                FieldName.LifeCycleConfig.FIELD_TRANSITION_SCHEDULE_UPDATE_TIME);
        customized = BsonUtils.getBooleanChecked(obj,
                FieldName.LifeCycleConfig.FIELD_TRANSITION_SCHEDULE_CUSTOMIZED);
        enable = BsonUtils.getBooleanChecked(obj,
                FieldName.LifeCycleConfig.FIELD_TRANSITION_SCHEDULE_ENABLE);
        preferredRegion = BsonUtils.getStringChecked(obj,
                FieldName.LifeCycleConfig.FIELD_TRANSITION_SCHEDULE_PREFERRED_REGION);
        preferredZone = BsonUtils.getStringChecked(obj,
                FieldName.LifeCycleConfig.FIELD_TRANSITION_SCHEDULE_PREFERRED_ZONE);

        BasicBSONList l = (BasicBSONList) obj
                .get(FieldName.LifeCycleConfig.FIELD_TRANSITION_SCHEDULE_IDS);
        scheduleIds = new ArrayList<ScmId>();
        for (Object temp : l) {
            scheduleIds.add(new ScmId((String) temp, false));
        }

        BSONObject temp = (BSONObject) obj
                .get(FieldName.LifeCycleConfig.FIELD_TRANSITION_SCHEDULE_TRANSITION);
        transition = ScmLifeCycleTransition.fromRecord(temp);

    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getWorkspace() {
        return workspace;
    }

    @Override
    public String getCreateUser() {
        return createUser;
    }

    @Override
    public String getUpdateUser() {
        return updateUser;
    }

    @Override
    public long getCreateTime() {
        return createTime;
    }

    @Override
    public long getUpdateTime() {
        return updateTime;
    }

    @Override
    public boolean isCustomized() {
        return customized;
    }

    @Override
    public boolean isEnable() {
        return enable;
    }

    @Override
    public String getPreferredRegion() {
        return preferredRegion;
    }

    @Override
    public String getPreferredZone() {
        return preferredZone;
    }

    @Override
    public List<ScmId> getScheduleIds() {
        return scheduleIds;
    }

    @Override
    public ScmLifeCycleTransition getTransition() {
        return transition;
    }

    @Override
    public void disable() throws ScmException {
        _enable(false);
    }

    @Override
    public void enable() throws ScmException {
        _enable(true);
    }

    private void _enable(boolean enable) throws ScmException {
        session.getDispatcher().updateWsTransitionStatus(workspace, transition.getName(), enable);
        this.enable = enable;
    }
}
