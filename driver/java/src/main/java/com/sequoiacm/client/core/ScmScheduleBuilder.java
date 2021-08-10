package com.sequoiacm.client.core;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import org.bson.BSONObject;

public class ScmScheduleBuilder {
    private final ScmSession ss;
    private ScheduleType type;
    private String ws;
    private String cron;
    private String name;
    private String description;
    private ScmScheduleContent content;
    private boolean enable;
    private String preferredRegion;
    private String preferredZone;

    ScmScheduleBuilder(ScmSession ss) throws ScmException {
        assertNotNull(ss, "session is null");
        this.ss = ss;
        this.enable = true;
        this.preferredRegion = ss.getPreferredRegion();
        this.preferredZone = ss.getPreferredZone();
    }

    private void assertNotNull(Object value, String msg) throws ScmException {
        if (value == null) {
            throw new ScmException(ScmError.INVALID_ARGUMENT, msg);
        }
    }

    /**
     * Sets schedule type.
     * 
     * @param type
     *            schedule type
     * @return builder.
     */
    public ScmScheduleBuilder type(ScheduleType type) {
        this.type = type;
        return this;
    }

    /**
     * Sets schedule workspace.
     * 
     * @param ws
     *            workspace name.
     * @return builder.
     */
    public ScmScheduleBuilder workspace(String ws) {
        this.ws = ws;
        return this;
    }

    /**
     * Set schedule cron.
     * 
     * @param cron
     *            schedule cron.
     * @return builder.
     */
    public ScmScheduleBuilder cron(String cron) {
        this.cron = cron;
        return this;
    }

    /**
     * Sets schedule name.
     * 
     * @param name
     *            schedule name.
     * @return builder
     */
    public ScmScheduleBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets schedule description.
     * 
     * @param desc
     *            schedule description.
     * @return builder.
     */
    public ScmScheduleBuilder description(String desc) {
        this.description = desc;
        return this;
    }

    /**
     * Sets schedule content.
     * 
     * @param content
     *            schedule content
     * @return builder.
     */
    public ScmScheduleBuilder content(ScmScheduleContent content) {
        this.content = content;
        return this;
    }

    /**
     * Is enabled the schedule.
     * 
     * @param enable
     *            enable the schedule or not.
     * @return builder.
     */
    public ScmScheduleBuilder enable(boolean enable) {
        this.enable = enable;
        return this;
    }

    /**
     * Sets the preferred region.
     * 
     * @param region
     *            region name.
     * @return builder.
     */
    public ScmScheduleBuilder preferredRegion(String region) {
        this.preferredRegion = region;
        return this;
    }

    /**
     * Sets the preferred zone.
     * 
     * @param zone
     *            zone name.
     * @return builder.
     */
    public ScmScheduleBuilder preferredZone(String zone) {
        this.preferredZone = zone;
        return this;
    }

    /**
     * Creates the schedule.
     * 
     * @return schedule instance.
     * @throws ScmException
     *             if error happens.
     */
    public ScmSchedule build() throws ScmException {
        assertNotNull(ws, "workspace is null");
        assertNotNull(type, "type is null");
        assertNotNull(name, "name is null");
        assertNotNull(content, "content is null");
        BSONObject ret = ss.getDispatcher().createSchedule(ws, type, name, description,
                content.toBSONObject(), cron, enable, preferredRegion, preferredZone);
        return new ScmScheduleImpl(ss, ret);
    }

}
