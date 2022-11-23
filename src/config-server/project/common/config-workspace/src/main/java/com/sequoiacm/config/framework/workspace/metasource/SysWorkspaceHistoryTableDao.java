package com.sequoiacm.config.framework.workspace.metasource;

import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.exception.MetasourceException;

public interface SysWorkspaceHistoryTableDao extends TableDao {
    void initWorkspaceHistoryTable() throws MetasourceException;
}
