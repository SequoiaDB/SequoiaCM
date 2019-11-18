package com.sequoiacm.cloud.adminserver.core;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.model.WorkspaceInfo;

public interface ScmStatistics {

    void doStatistics(boolean needBacktrace) throws StatisticsException;
    
    void refresh(WorkspaceInfo... workspaces) throws StatisticsException;
}
