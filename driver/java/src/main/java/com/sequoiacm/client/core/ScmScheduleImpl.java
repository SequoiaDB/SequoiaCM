package com.sequoiacm.client.core;

import java.util.Date;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.client.common.RestDefine;
import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleBasicInfo;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;

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
