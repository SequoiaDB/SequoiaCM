package com.sequoiacm.client.element;

import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;

import com.sequoiacm.client.common.RestDefine;
import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.exception.ScmException;

/**
 * The brief and partial information of Schedule.
 */
public class ScmScheduleBasicInfo {
    private ScmId id;
    private String workspace;
    private String name;
    private String desc;
    private String cron;
    private ScheduleType type;
    private boolean enable = true;
    private String preferredRegion;
    private String preferredZone;
    private String transition;

    /**
     * Create a instance of ScmScheduleBasicInfo.
     *
     * @param info
     *            a bson containing basic information about scm schedule.
     * @throws ScmException
     *             if error happens.
     */
    public ScmScheduleBasicInfo(BSONObject info) throws ScmException {
        Object temp = null;
        temp = info.get(RestDefine.RestKey.NAME);
        if (null != temp) {
            setName((String) temp);
        }

        temp = info.get(RestDefine.RestKey.ID);
        if (null != temp) {
            setId(new ScmId((String) temp, false));
        }

        temp = info.get(RestDefine.RestKey.WORKSPACE);
        if (null != temp) {
            setWorkspace((String) temp);
        }

        temp = info.get(RestDefine.RestKey.DESC);
        if (null != temp) {
            setDesc((String) temp);
        }

        temp = info.get(RestDefine.RestKey.CRON);
        if (null != temp) {
            setCron((String) temp);
        }

        temp = info.get(RestDefine.RestKey.TYPE);
        if (null != temp) {
            setType(ScheduleType.getType((String) temp));
        }

        temp = info.get(RestDefine.RestKey.ENABLE);
        if (null != temp) {
            setEnable((Boolean) temp);
        }

        setPreferredRegion(BsonUtils.getString(info, RestDefine.RestKey.PREFERRED_REGION));
        setPreferredZone(BsonUtils.getString(info, RestDefine.RestKey.PREFERRED_ZONE));

        temp = info.get(RestDefine.RestKey.TRANSITION_NAME);
        if (null != temp){
            setTransition((String) temp);
        }
    }

    /**
     * Sets workspace name.
     *
     * @param workspace
     *            workspace name.
     */
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    /**
     * Gets workspace name.
     *
     * @return workspace name.
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * Sets id.
     *
     * @param id
     *            schedule id.
     */
    public void setId(ScmId id) {
        this.id = id;
    }

    /**
     * Sets name.
     *
     * @param name
     *            schedule name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets description.
     *
     * @param desc
     *            description.
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    /**
     * Sets cron.
     *
     * @param cron
     *            cron string.
     */
    public void setCron(String cron) {
        this.cron = cron;
    }

    /**
     * Sets type.
     *
     * @param type
     *            schedule type.
     */
    public void setType(ScheduleType type) {
        this.type = type;
    }

    /**
     * Gets schedule id.
     *
     * @return id.
     */
    public ScmId getId() {
        return id;
    }

    /**
     * Gets schedule name.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets description.
     *
     * @return desc.
     */
    public String getDesc() {
        return desc;
    }

    /**
     * Gets type.
     *
     * @return type.
     */
    public ScheduleType getType() {
        return type;
    }

    /**
     * Gets the cron.
     *
     * @return cron.
     */
    public String getCron() {
        return cron;
    }

    /**
     * Is enable.
     *
     * @return true or false.
     */
    public boolean isEnable() {
        return enable;
    }

    /**
     * Sets enable, for internal use.
     *
     * @param enable
     *            true or false.
     */
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    /**
     * Sets preferred region, for internal use.
     * 
     * @param preferredRegion
     *            region name.
     */
    public void setPreferredRegion(String preferredRegion) {
        this.preferredRegion = preferredRegion;
    }

    /**
     * Gets preferred region.
     * 
     * @return region name.
     */
    public String getPreferredRegion() {
        return preferredRegion;
    }

    /**
     * Sets preferred zone, for internal use.
     * 
     * @param preferredZone
     *            zone name.
     */
    public void setPreferredZone(String preferredZone) {
        this.preferredZone = preferredZone;
    }

    /**
     * Gets preferred zone.
     * 
     * @return zone name.
     */
    public String getPreferredZone() {
        return preferredZone;
    }

    public String getTransition() {
        return transition;
    }

    public void setTransition(String transition) {
        this.transition = transition;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ScmAttributeName.Schedule.ID).append(":").append(id).append(",")
                .append(ScmAttributeName.Schedule.NAME).append(":").append(name).append(",")
                .append(ScmAttributeName.Schedule.TYPE).append(":").append(type).append(",")
                .append(ScmAttributeName.Schedule.DESC).append(":").append(desc).append(",")
                .append(ScmAttributeName.Schedule.DESC).append(":").append(cron).append(",")
                .append(ScmAttributeName.Schedule.ENABLE).append(":").append(enable)
                .append(ScmAttributeName.Schedule.PREFERRED_REGION).append(":")
                .append(preferredRegion).append(ScmAttributeName.Schedule.PREFERRED_ZONE)
                .append(":").append(preferredZone);
        return sb.toString();
    }
}
