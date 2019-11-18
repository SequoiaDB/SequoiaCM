package com.sequoiacm.client.core;

import java.util.Date;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.exception.ScmException;

/**
 * Scm Schedule.
 */
public interface ScmSchedule {

    /**
     * Gets the id of the schedule.
     *
     * @return schedule id.
     */
    public ScmId getId();

    /**
     * Gets the name of the schedule.
     *
     * @return schedule name.
     */
    public String getName();

    /**
     * Gets the description of the schedule.
     *
     * @return description.
     */
    public String getDesc();

    /**
     * Gets the type of the schedule.
     *
     * @return schedule type.
     */
    public ScheduleType getType();

    /**
     * Gets the schedule content of the schedule.
     *
     * @return scheduel content.
     */
    public ScmScheduleContent getContent();

    /**
     * Gets the Cron of the schedule.
     *
     * @return cron.
     */
    public String getCron();

    /**
     * Gets the created user of the schedule.
     *
     * @return created user.
     */
    public String getCreaateUser();

    /**
     * Gets the created date of the schedule.
     *
     * @return created date.
     */
    public Date getCreateDate();

    /**
     * Gets the workspace name of the schedule.
     *
     * @return workspace name.
     */
    public String getWorkspace();

    /**
     * Updates the name of the schedule.
     *
     * @param name
     *            new name.
     * @throws ScmException
     *             if error happens.
     */
    public void updateName(String name) throws ScmException;

    /**
     * Updates the description of the schedule.
     *
     * @param desc
     *            description.
     * @throws ScmException
     *             if error happens.
     */
    public void updateDesc(String desc) throws ScmException;

    /**
     * Updates the content of the schedule.
     *
     * @param content
     *            new content.
     * @throws ScmException
     *             if error happens.
     */
    public void updateContent(ScmScheduleContent content) throws ScmException;

    /**
     * Updates the cron of the schedule.
     *
     * @param cron
     *            new cron.
     * @throws ScmException
     *             if error happens.
     */
    public void updateCron(String cron) throws ScmException;

    /**
     * Updates schedule type and cotnent.
     *
     * @param type
     *            new type.
     * @param content
     *            new content.
     * @throws ScmException
     *             if error happens.
     */
    public void updateSchedule(ScheduleType type, ScmScheduleContent content) throws ScmException;

    /**
     * Deletes current schedule.
     *
     * @throws ScmException
     *             if error happens.
     */
    public void delete() throws ScmException;

    /**
     * Disables the current schedule
     *
     * @throws ScmException
     *             if error happens.
     */
    public void disable() throws ScmException;

    /**
     * Enables the current schedule
     *
     * @throws ScmException
     *             if error happens.
     */
    public void enable() throws ScmException;

    /**
     * Tests the schedule is enable.
     *
     * @return true or false.
     */
    public boolean isEnable();
}
