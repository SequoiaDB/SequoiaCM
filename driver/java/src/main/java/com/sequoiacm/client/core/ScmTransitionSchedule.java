package com.sequoiacm.client.core;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.client.exception.ScmException;

import java.util.List;

/**
 * The workspace' transition info.
 */
public interface ScmTransitionSchedule {
    /**
     * Return the transition schedule id.
     * 
     * @return the transition schedule id.
     */
    public String getId();

    /**
     * Return the workspace name.
     * 
     * @return the workspace name.
     */
    public String getWorkspace();

    /**
     * Return the username of the user who apply transition.
     * 
     * @return the username.
     */
    public String getCreateUser();

    /**
     * Return the username of the user who update transition info.
     * 
     * @return the username.
     */
    public String getUpdateUser();

    /**
     * Return the created time when aply transition.
     * 
     * @return the created time.
     */
    public long getCreateTime();

    /**
     * Return the update time when update transition info.
     * 
     * @return the update time.
     */
    public long getUpdateTime();

    /**
     * Return the workspace' transition customized info.
     * 
     * @return is customized.
     */
    public boolean isCustomized();

    /**
     * Return the workspace' transition enable info.
     * 
     * @return is enable.
     */
    public boolean isEnable();

    /**
     * Return the workspace' transition preferredRegion info.
     * 
     * @return preferredRegion info.
     */
    public String getPreferredRegion();

    /**
     * Return the workspace' transition preferredZone info.
     * 
     * @return preferredZone info.
     */
    public String getPreferredZone();

    /**
     * Return the workspace' transition with schedule ScmId list.
     * 
     * @return ScmId list
     */
    public List<ScmId> getScheduleIds();

    /**
     * Return the transition info.
     * 
     * @return ScmLifeCycleTransition.
     */
    public ScmLifeCycleTransition getTransition();

    /**
     * Disable workspace's transition.
     * 
     * @throws ScmException
     *             If error happens.
     */
    public void disable()throws ScmException;

    /**
     * Enable workspace's transition.
     * 
     * @throws ScmException
     *             If error happens.
     */
    public void enable()throws ScmException;
}
