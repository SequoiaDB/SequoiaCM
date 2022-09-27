package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
import com.sequoiacm.infrastructure.lock.ScmLockManager;
import com.sequoiacm.metasource.MetaSource;
import com.sequoiacm.sequoiadb.metaoperation.MetaDataOperator;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.SDBError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataoperation.ScmDataDeletor;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.sequoiadb.SequoiadbException;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;

public class SdbDataDeletorImpl implements ScmDataDeletor {
    private static final Logger logger = LoggerFactory.getLogger(SdbDataDeletorImpl.class);

    private int siteId;
    private String csName;
    private String clName;
    private String lobId;
    private SdbDataService service;
    private MetaDataOperator metaDataOperator;
    private String siteName;
    private ScmLockManager lockManager;

    SdbDataDeletorImpl(int siteId, String siteName, String csName, String clName, String wsName,
            String id, ScmService service, MetaSource metaSource, ScmLockManager lockManager)
            throws SequoiadbException {
        this.siteId = siteId;
        this.csName = csName;
        this.clName = clName;
        this.lobId = id;
        this.service = (SdbDataService)service;
        this.siteName = siteName;
        this.lockManager = lockManager;
        this.metaDataOperator = new MetaDataOperator(metaSource, wsName, siteName, siteId);
    }

    @Override
    @SlowLog(operation = "deleteData", extras = @SlowLogExtra(name = "deleteLobId", data = "lobId"))
    public void delete() throws SequoiadbException {
        try {
            service.removeLob(csName, clName, lobId);
        }
        catch (SequoiadbException e) {
            if (e.getDatabaseError() == SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()
                    || e.getDatabaseError() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                boolean recovered = doRecover();
                if (recovered) {
                    delete();
                    return;
                }
            }
            throw e;
        }
        catch (Exception e) {
            logger.error("delete lob failed:siteId=" + siteId + ",csName=" + csName + ",clName="
                    + clName + ",lobId=" + lobId);
            throw new SequoiadbException(
                    "delete lob failed:siteId=" + siteId + ",csName=" + csName + ",clName="
                            + clName + ",lobId=" + lobId, e);
        }
    }

    private boolean doRecover() throws SequoiadbException {
        Sequoiadb sequoiadb = service.getSequoiadb();
        try {
            return SdbCsRecycleHelper.recoverIfNeeded(sequoiadb, siteName, csName, metaDataOperator,
                    lockManager);
        }
        finally {
            if (sequoiadb != null) {
                service.releaseSequoiadb(sequoiadb);
            }
        }
    }

}
