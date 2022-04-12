package com.sequoiacm.config.framework.workspace.metasource;

import org.bson.BSONObject;

import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;

public interface WorkspaceMetaSerivce {

    SysWorkspaceTableDao getSysWorkspaceTable(Transaction transaction);

    void createWorkspaceMetaTable(WorkspaceConfig wsConfig)
            throws MetasourceException;

    TableDao getWorkspaceDirTableDao(String wsName, Transaction transaction)
            throws MetasourceException;

    TableDao getDataTableNameHistoryDao() throws MetasourceException;

    void deleteWorkspaceMetaTable(String wsName) throws MetasourceException;

    void deleteWorkspaceMetaTable(BSONObject wsRecord) throws MetasourceException;
    TableDao getWorkspaceHistoryTable();
}
