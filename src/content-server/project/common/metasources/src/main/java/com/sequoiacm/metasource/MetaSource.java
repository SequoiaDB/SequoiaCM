package com.sequoiacm.metasource;

import com.sequoiacm.metasource.config.MetaSourceLocation;

public interface MetaSource {
    MetaAccessor getSiteAccessor();

    public MetaAccessor getServerAccessor();

    public MetaWorkspaceAccessor getWorkspaceAccessor();

    public MetaAccessor getWorkspaceAccessor(TransactionContext transaction);

    public MetaAccessor getUserAccessor();

    public MetaAccessor getStrategyAccessor();

    public MetaAccessor getAuditAccessor();

    public MetaTransLogAccessor getTransLogAccessor(String wsName);

    public MetaFileAccessor getFileAccessor(MetaSourceLocation location, String wsName,
            TransactionContext context);

    public MetaFileHistoryAccessor getFileHistoryAccessor(MetaSourceLocation location,
            String wsName, TransactionContext context);

    public MetaBreakpointFileAccessor getBreakpointFileAccessor(String wsName,
            TransactionContext context);

    public MetaSessionAccessor getSessionAccessor();

    public MetaTaskAccessor getTaskAccessor();

    public MetaClassAccessor getClassAccessor(String wsName, TransactionContext context);

    public MetaAttrAccessor getAttrAccessor(String wsName);

    public MetaClassAttrRelAccessor getClassAttrRelAccessor(String wsName,
            TransactionContext context);

    public MetaBatchAccessor getBatchAccessor(String wsName, TransactionContext context);

    public MetaDirAccessor getDirAccessor(String wsName);

    public MetaDirAccessor getDirAccessor(String wsName, TransactionContext transactionContext);

    public TransactionContext createTransactionContext() throws ScmMetasourceException;

    public MetaRelAccessor getRelAccessor(String wsName, TransactionContext context);

    public MetaHistoryDataTableNameAccessor getDataTableNameHistoryAccessor()
            throws ScmMetasourceException;

    public void close();
}
