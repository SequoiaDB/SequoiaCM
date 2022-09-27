package com.sequoiacm.metasource.sequoiadb;

import java.util.List;

import com.sequoiacm.metasource.MetaSpaceRecyclingLogAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbMetaAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbSpaceRecyclingLogAccessor;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.metasource.ContentModuleMetaSource;
import com.sequoiacm.metasource.MetaAccessor;
import com.sequoiacm.metasource.MetaAttrAccessor;
import com.sequoiacm.metasource.MetaBatchAccessor;
import com.sequoiacm.metasource.MetaBreakpointFileAccessor;
import com.sequoiacm.metasource.MetaClassAccessor;
import com.sequoiacm.metasource.MetaClassAttrRelAccessor;
import com.sequoiacm.metasource.MetaDirAccessor;
import com.sequoiacm.metasource.MetaFileAccessor;
import com.sequoiacm.metasource.MetaFileHistoryAccessor;
import com.sequoiacm.metasource.MetaHistoryDataTableNameAccessor;
import com.sequoiacm.metasource.MetaRelAccessor;
import com.sequoiacm.metasource.MetaSessionAccessor;
import com.sequoiacm.metasource.MetaSourceDefine;
import com.sequoiacm.metasource.MetaTaskAccessor;
import com.sequoiacm.metasource.MetaTransLogAccessor;
import com.sequoiacm.metasource.MetaWorkspaceAccessor;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.metasource.config.MetaSourceLocation;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbAttrAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbAuditAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbBatchAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbBreakpointFileAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbClassAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbClassAttrRelAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbDataTableNameHistoryAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbDirAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbFileCurrentAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbFileHistoryAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbRelAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbServerAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbSessionAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbSiteAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbStrategyAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbTaskAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbTransLogAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbUserAccessor;
import com.sequoiacm.metasource.sequoiadb.accessor.SdbWorkspaceAccessor;
import com.sequoiacm.metasource.sequoiadb.config.SdbMetaSourceLocation;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.net.ConfigOptions;

public class SdbMetaSource implements ContentModuleMetaSource {
    private static final Logger logger = LoggerFactory.getLogger(SdbMetaSource.class);

    private SdbDataSourceWrapper ms = null;

    public SdbMetaSource(List<String> urlList, String user, String passwd, ConfigOptions connConf,
            DatasourceOptions datasourceConf) throws SdbMetasourceException {
        ms = new SdbDataSourceWrapper(urlList, user, passwd, connConf, datasourceConf);
        ensureTable();
    }

    private void ensureTable() throws SdbMetasourceException {
        BasicBSONObject clOption = new BasicBSONObject();
        ensureCollection(MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_SPACE_RECYCLING_LOG, clOption);
    }

    @Override
    public MetaAccessor getSiteAccessor() {
        return new SdbSiteAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_SITE);
    }

    @Override
    public MetaAccessor getServerAccessor() {
        return new SdbServerAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_CONTENTSERVER);
    }

    @Override
    public MetaWorkspaceAccessor getWorkspaceAccessor(TransactionContext transactionContext) {
        return new SdbWorkspaceAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_WORKSPACE, transactionContext);
    }

    @Override
    public MetaWorkspaceAccessor getWorkspaceAccessor() {
        return getWorkspaceAccessor(null);
    }

    @Override
    public MetaSessionAccessor getSessionAccessor() {
        return new SdbSessionAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_SESSION);
    }

    @Override
    public MetaAccessor getUserAccessor() {
        return new SdbUserAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_USER);
    }

    @Override
    public MetaAccessor getStrategyAccessor() {
        return new SdbStrategyAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_STRATEGY);
    }

    @Override
    public MetaTransLogAccessor getTransLogAccessor(String wsName) {
        return new SdbTransLogAccessor(this, wsName + "_META",
                MetaSourceDefine.WorkspaceCLName.CL_TRANSACTION_LOG);
    }

    @Override
    public MetaTaskAccessor getTaskAccessor() {
        return new SdbTaskAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_TASK);
    }

    @Override
    public MetaFileAccessor getFileAccessor(MetaSourceLocation location, String wsName,
            TransactionContext context) {
        return new SdbFileCurrentAccessor((SdbMetaSourceLocation) location, this, wsName + "_META",
                MetaSourceDefine.WorkspaceCLName.CL_FILE, context);
    }

    @Override
    public MetaFileHistoryAccessor getFileHistoryAccessor(MetaSourceLocation location,
            String wsName, TransactionContext context) {
        return new SdbFileHistoryAccessor((SdbMetaSourceLocation) location, wsName, this,
                wsName + "_META",
                MetaSourceDefine.WorkspaceCLName.CL_FILE_HISTORY, context);
    }

    @Override
    public MetaBreakpointFileAccessor getBreakpointFileAccessor(String wsName,
            TransactionContext context) {
        return new SdbBreakpointFileAccessor(this, wsName + "_META",
                MetaSourceDefine.WorkspaceCLName.CL_BREAKPOINT_FILE, context);
    }

    @Override
    public MetaDirAccessor getDirAccessor(String wsName) {
        return getDirAccessor(wsName, null);
    }

    @Override
    public MetaDirAccessor getDirAccessor(String wsName, TransactionContext transactionContext) {
        return new SdbDirAccessor(this, wsName + "_META",
                MetaSourceDefine.WorkspaceCLName.CL_DIRECTORY, transactionContext);
    }

    @Override
    public MetaBatchAccessor getBatchAccessor(String wsName, TransactionContext context) {
        return new SdbBatchAccessor(this, wsName + "_META",
                MetaSourceDefine.WorkspaceCLName.CL_BATCH, context);
    }

    public Sequoiadb getConnection() throws SdbMetasourceException {
        return ms.getConnection();
    }

    public void releaseConnection(Sequoiadb sdb) {
        ms.releaseConnection(sdb);
    }

    @Override
    public void close() {
        try {
            if (null != ms) {
                ms.clear();
                ms = null;
            }
        }
        catch (Exception e) {
            logger.warn("close sdbFactory failed", e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ms.toString());
        return sb.toString();
    }

    @Override
    public MetaClassAccessor getClassAccessor(String wsName, TransactionContext context) {
        return new SdbClassAccessor(this, wsName + "_META",
                MetaSourceDefine.WorkspaceCLName.CL_CLASS, context);
    }

    @Override
    public MetaAttrAccessor getAttrAccessor(String wsName) {
        return new SdbAttrAccessor(this, wsName + "_META",
                MetaSourceDefine.WorkspaceCLName.CL_ATTRIBUTE);
    }

    @Override
    public MetaClassAttrRelAccessor getClassAttrRelAccessor(String wsName,
            TransactionContext context) {
        return new SdbClassAttrRelAccessor(this, wsName + "_META",
                MetaSourceDefine.WorkspaceCLName.CL_CLASS_ATTR_REL, context);
    }

    @Override
    public MetaAccessor createMetaAccessor(String tableName) throws ScmMetasourceException {
        return createMetaAccessor(tableName, null);
    }

    @Override
    public MetaAccessor createMetaAccessor(String tableName, TransactionContext context)
            throws ScmMetasourceException {
        String[] cscl = tableName.split("\\.");
        if (cscl.length != 2) {
            throw new ScmMetasourceException("invalid sdb table name:" + tableName);
        }
        return new SdbMetaAccessor(this, cscl[0], cscl[1], context);
    }

    @Override
    public TransactionContext createTransactionContext() throws SdbMetasourceException {
        return new SdbTransactionContext(this);
    }

    @Override
    public MetaRelAccessor getRelAccessor(String wsName, TransactionContext context) {
        return new SdbRelAccessor(this, wsName + "_META",
                MetaSourceDefine.WorkspaceCLName.CL_FILE_RELATION, context);
    }

    @Override
    public MetaAccessor getAuditAccessor() {
        return new SdbAuditAccessor(this, MetaSourceDefine.CsName.CS_SCMAUDIT,
                MetaSourceDefine.SystemClName.CL_AUDIT);
    }

    @Override
    public MetaHistoryDataTableNameAccessor getDataTableNameHistoryAccessor()
            throws ScmMetasourceException {
        return new SdbDataTableNameHistoryAccessor(this);
    }

    @Override
    public void activeHandler(IMetaSourceHandler handler) {
        handler.refresh(ms.getDataSource());
    }

    @Override
    public MetaSpaceRecyclingLogAccessor getSpaceRecyclingLogAccessor() {
        return new SdbSpaceRecyclingLogAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_SPACE_RECYCLING_LOG);
    }

    public void ensureCollection(String csName, String clName, BSONObject clOption)
            throws SdbMetasourceException {
        Sequoiadb db = getConnection();
        try {
            CollectionSpace cs = db.getCollectionSpace(csName);
            if (cs.isCollectionExist(clName)) {
                return;
            }
            cs.createCollection(clName, clOption);
            logger.info("create collection:{}.{}, option={}", csName, clName, clOption);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_EXIST.getErrorCode()) {
                return;
            }
            throw new SdbMetasourceException(e.getErrorCode(),
                    "failed to create cl:" + csName + "." + clName + ", option=" + clOption, e);
        }
        finally {
            releaseConnection(db);
        }
    }
}