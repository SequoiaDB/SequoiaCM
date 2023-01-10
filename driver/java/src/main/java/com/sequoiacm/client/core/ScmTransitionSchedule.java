package com.sequoiacm.client.core;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.lifecycle.ScmLifeCycleTransition;
import com.sequoiacm.client.exception.ScmException;

import java.util.List;

public interface ScmTransitionSchedule {
    public String getId();

    public String getWorkspace();

    public String getCreateUser();

    public String getUpdateUser();

    public long getCreateTime();

    public long getUpdateTime();

    public boolean isCustomized();

    public boolean isEnable();

    public String getPreferredRegion();

    public String getPreferredZone();

    public List<ScmId> getScheduleIds();

    public ScmLifeCycleTransition getTransition();

    public void disable()throws ScmException;

    public void enable()throws ScmException;
}
