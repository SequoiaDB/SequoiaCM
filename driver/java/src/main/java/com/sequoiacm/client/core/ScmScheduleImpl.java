package com.sequoiacm.client.core;

import com.sequoiacm.client.common.RestDefine;
import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.dispatcher.BsonReader;
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.BsonConverter;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class ScmScheduleImpl implements ScmSchedule {
    private ScmSession ss;

    private ScmScheduleBasicInfo basicInfo;
    private ScmScheduleContent content;
    private String createUser;
    private Date createDate;

    public ScmScheduleImpl(ScmSession ss, BSONObject info) throws ScmException {
        this.ss = ss;

        basicInfo = new ScmScheduleBasicInfo(info);

        Object temp = null;
        temp = info.get(RestDefine.RestKey.CONTENT);
        if (null != temp) {
            ScmScheduleContent tmpContent = null;
            switch (getType()) {
                case COPY_FILE:
                    tmpContent = new ScmScheduleCopyFileContent((BSONObject) temp);
                    break;

                case CLEAN_FILE:
                    tmpContent = new ScmScheduleCleanFileContent((BSONObject) temp);
                    break;

                default:
                    break;
            }

            setContent(tmpContent);
        }

        temp = info.get(RestDefine.RestKey.CREATE_USER);
        if (null != temp) {
            setCreateUser((String) temp);
        }

        temp = info.get(RestDefine.RestKey.CREATE_TIME);
        if (null != temp) {
            setCreateDate((new Date((Long) temp)));
        }
    }

    public void setContent(ScmScheduleContent content) {
        this.content = content;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Override
    public ScmId getId() {
        return basicInfo.getId();
    }

    @Override
    public String getName() {
        return basicInfo.getName();
    }

    @Override
    public String getDesc() {
        return basicInfo.getDesc();
    }

    @Override
    public ScheduleType getType() {
        return basicInfo.getType();
    }

    @Override
    public ScmScheduleContent getContent() {
        return content;
    }

    @Override
    public String getCron() {
        return basicInfo.getCron();
    }

    @Override
    public String getCreaateUser() {
        return createUser;
    }

    @Override
    public Date getCreateDate() {
        return createDate;
    }

    @Override
    public String getWorkspace() {
        return basicInfo.getWorkspace();
    }

    @Override
    public void updateName(String name) throws ScmException {
        if (null == name) {
            throw new ScmInvalidArgumentException("name can't be null");
        }

        BSONObject newValue = new BasicBSONObject();
        newValue.put(ScmAttributeName.Schedule.NAME, name);
        ss.getDispatcher().updateSchedule(getId().get(), newValue);
        basicInfo.setName(name);
    }

    @Override
    public void updateDesc(String desc) throws ScmException {
        if (null == desc) {
            throw new ScmInvalidArgumentException("desc can't be null");
        }

        BSONObject newValue = new BasicBSONObject();
        newValue.put(ScmAttributeName.Schedule.DESC, desc);
        ss.getDispatcher().updateSchedule(getId().get(), newValue);
        basicInfo.setDesc(desc);
    }

    @Override
    public void updateContent(ScmScheduleContent content) throws ScmException {
        if (null == content) {
            throw new ScmInvalidArgumentException("content can't be null");
        }

        BSONObject newValue = new BasicBSONObject();
        newValue.put(ScmAttributeName.Schedule.CONTENT, content.toBSONObject());
        ss.getDispatcher().updateSchedule(getId().get(), newValue);
        this.content = content;
    }

    @Override
    public void updateCron(String cron) throws ScmException {
        if (null == cron) {
            throw new ScmInvalidArgumentException("cron can't be null");
        }

        BSONObject newValue = new BasicBSONObject();
        newValue.put(ScmAttributeName.Schedule.CRON, cron);
        ss.getDispatcher().updateSchedule(getId().get(), newValue);
        basicInfo.setCron(cron);
    }

    @Override
    public void updateSchedule(ScheduleType type, ScmScheduleContent content) throws ScmException {
        if (null == content) {
            throw new ScmInvalidArgumentException("content can't be null");
        }

        if (null == type) {
            throw new ScmInvalidArgumentException("type can't be null");
        }

        BSONObject newValue = new BasicBSONObject();
        newValue.put(ScmAttributeName.Schedule.TYPE, type.getName());
        newValue.put(ScmAttributeName.Schedule.CONTENT, content.toBSONObject());
        ss.getDispatcher().updateSchedule(getId().get(), newValue);
        this.content = content;
        basicInfo.setType(type);
    }

    @Override
    public void updateSchedule(String name, String cron, String workspace, String dec,
            ScheduleType type, Boolean enable, String preferredRegion, String preferredZone,
            ScmScheduleContent content) throws ScmException {
        BSONObject newValue = new BasicBSONObject();
        newValue.put(ScmAttributeName.Schedule.NAME, name);
        newValue.put(ScmAttributeName.Schedule.CRON, cron);
        newValue.put(ScmAttributeName.Schedule.WORKSPACE, workspace);
        newValue.put(ScmAttributeName.Schedule.DESC, dec);
        newValue.put(ScmAttributeName.Schedule.TYPE, type.getName());
        newValue.put(ScmAttributeName.Schedule.ENABLE, enable);
        newValue.put(ScmAttributeName.Schedule.PREFERRED_REGION, preferredRegion);
        newValue.put(ScmAttributeName.Schedule.PREFERRED_ZONE, preferredZone);
        BSONObject contentBson = content == null  ? null : content.toBSONObject();
        newValue.put(ScmAttributeName.Schedule.CONTENT, contentBson);
        ss.getDispatcher().updateSchedule(getId().get(), newValue);
        if (name != null) {
            basicInfo.setName(name);
        }
        if (cron != null) {
            basicInfo.setCron(cron);
        }
        if (workspace != null) {
            basicInfo.setWorkspace(workspace);
        }
        if (content != null) {
            this.content = content;
        }
        if (type != null) {
            basicInfo.setType(type);
        }
        if (enable != null) {
            basicInfo.setEnable(enable);
        }
        if (dec != null) {
            basicInfo.setDesc(dec);
        }
        if (preferredRegion != null) {
            basicInfo.setPreferredRegion(preferredRegion);
        }
        if (preferredZone != null) {
            basicInfo.setPreferredZone(preferredZone);
        }
    }

    private void checkArgNotNull(String name, Object obj) throws ScmInvalidArgumentException {
        if (null == obj) {
            throw new ScmInvalidArgumentException(name + " can't be null");
        }
    }

    @Override
    public void delete() throws ScmException {
        ss.getDispatcher().deleteSchedule(getId().get());
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
        BSONObject newValue = new BasicBSONObject();
        newValue.put(ScmAttributeName.Schedule.ENABLE, enable);
        ss.getDispatcher().updateSchedule(getId().get(), newValue);
        basicInfo.setEnable(enable);
    }

    @Override
    public boolean isEnable() {
        return basicInfo.isEnable();
    }

    @Override
    public ScmTask getLatestTask() throws ScmException {
        List<ScmTask> tasks = getLatestTasks(1);
        if (tasks.size() >= 1) {
            return tasks.get(0);
        }
        return null;
    }

    @Override
    public List<ScmTask> getLatestTasks(int count) throws ScmException {
        BSONObject orderby = new BasicBSONObject();
        orderby.put(ScmAttributeName.Task.START_TIME, -1);
        return getTasks(null, orderby, 0, count);
    }

    @Override
    public List<ScmTask> getTasks(BSONObject extraCondition, BSONObject orderby, long skip,
            long limit) throws ScmException {
        ScmQueryBuilder builder = new ScmQueryBuilder();

        BSONObject condition = new BasicBSONObject();
        condition.put(ScmAttributeName.Task.SCHEDULE_ID, basicInfo.getId().get());
        builder.and(condition);

        if (null != extraCondition) {
            builder.and(extraCondition);
        }

        BsonReader reader = ss.getDispatcher().getTaskList(builder.get(), orderby,
                new BasicBSONObject(), skip, limit);
        ScmBsonCursor<ScmTask> cursor = new ScmBsonCursor<ScmTask>(reader,
                new BsonConverter<ScmTask>() {
                    @Override
                    public ScmTask convert(BSONObject obj) throws ScmException {
                        return new ScmTask(obj);
                    }
                });

        try {
            List<ScmTask> tasks = new ArrayList<ScmTask>();
            while (cursor.hasNext()) {
                tasks.add(cursor.getNext());
            }
            return tasks;
        }
        finally {
            cursor.close();
        }
    }

    @Override
    public String getPreferredRegion() {
        return basicInfo.getPreferredRegion();
    }

    @Override
    public String getPreferredZone() {
        return basicInfo.getPreferredZone();
    }

    @Override
    public void updatePreferredRegion(String region) throws ScmException {
        if (null == region) {
            throw new ScmInvalidArgumentException("region can't be null");
        }
        BSONObject newValue = new BasicBSONObject();
        newValue.put(ScmAttributeName.Schedule.PREFERRED_REGION, region);
        ss.getDispatcher().updateSchedule(getId().get(), newValue);
        basicInfo.setPreferredRegion(region);
    }

    @Override
    public void updatePreferredZone(String zone) throws ScmException {
        if (null == zone) {
            throw new ScmInvalidArgumentException("zone can't be null");
        }
        BSONObject newValue = new BasicBSONObject();
        newValue.put(ScmAttributeName.Schedule.PREFERRED_ZONE, zone);
        ss.getDispatcher().updateSchedule(getId().get(), newValue);
        basicInfo.setPreferredZone(zone);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ScmAttributeName.Schedule.ID).append(":").append(basicInfo.getId()).append(",")
                .append(ScmAttributeName.Schedule.NAME).append(":").append(basicInfo.getName())
                .append(",").append(ScmAttributeName.Schedule.DESC).append(":")
                .append(basicInfo.getDesc()).append(",").append(ScmAttributeName.Schedule.WORKSPACE)
                .append(":").append(basicInfo.getWorkspace()).append(",")
                .append(ScmAttributeName.Schedule.TYPE).append(":")
                .append(basicInfo.getType().getName()).append(",")
                .append(ScmAttributeName.Schedule.CREATE_USER).append(":").append(createUser)
                .append(",").append(ScmAttributeName.Schedule.CREATE_TIME).append(":")
                .append(createDate).append(",").append(ScmAttributeName.Schedule.ENABLE).append(":")
                .append(basicInfo.isEnable()).append(",").append(ScmAttributeName.Schedule.CONTENT)
                .append(":").append(content.toBSONObject().toString());

        return sb.toString();
    }

}
