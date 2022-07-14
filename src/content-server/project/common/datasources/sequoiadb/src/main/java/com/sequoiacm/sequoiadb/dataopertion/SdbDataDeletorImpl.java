package com.sequoiacm.sequoiadb.dataopertion;

import com.sequoiacm.infrastructure.common.annotation.SlowLog;
import com.sequoiacm.infrastructure.common.annotation.SlowLogExtra;
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

    SdbDataDeletorImpl(int siteId, String csName, String clName, String id, ScmService service)
            throws SequoiadbException {
        this.siteId = siteId;
        this.csName = csName;
        this.clName = clName;
        this.lobId = id;
        this.service = (SdbDataService)service;
    }

    @Override
    @SlowLog(operation = "deleteData", extras = @SlowLogExtra(name = "deleteLobId", data = "lobId"))
    public void delete() throws SequoiadbException {
        try {
            service.removeLob(csName, clName, lobId);
        }
        catch (SequoiadbException e) {
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

}
