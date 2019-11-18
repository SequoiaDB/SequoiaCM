package com.sequoiacm.cloud.adminserver.dao;

import java.util.List;

import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.model.WorkspaceInfo;

public interface WorkspaceDao {
    public List<WorkspaceInfo> query() throws StatisticsException;

    public WorkspaceInfo queryByName(String wsName) throws StatisticsException;
}
