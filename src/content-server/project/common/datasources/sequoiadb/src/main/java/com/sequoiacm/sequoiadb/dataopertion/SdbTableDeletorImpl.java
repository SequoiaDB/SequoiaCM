package com.sequoiacm.sequoiadb.dataopertion;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.ScmDatasourceException;
import com.sequoiacm.datasource.dataoperation.ScmDataTableDeletor;
import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.sequoiadb.dataservice.SdbDataService;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

public class SdbTableDeletorImpl implements ScmDataTableDeletor {
    private static final Logger logger = LoggerFactory.getLogger(ScmDataTableDeletor.class);

    private List<String> tableNames;
    private SdbDataService service;

    public SdbTableDeletorImpl(List<String> tableNames, ScmService service) {
        this.tableNames = tableNames;
        this.service = (SdbDataService) service;
    }

    @Override
    public void delete() throws ScmDatasourceException {
        Sequoiadb db = service.getSequoiadb();
        try {
            for (String csName : tableNames) {
                try {
                    db.dropCollectionSpace(csName);
                    logger.info("remove sdb data table:csName=" + csName);
                }
                catch (BaseException e) {
                    if (e.getErrorCode() != SDBError.SDB_DMS_CS_NOTEXIST.getErrorCode()) {
                        logger.warn("remove sdb data data table failed:csName={}", csName, e);
                    }
                }
            }
        }
        catch (Exception e) {
            throw new ScmDatasourceException("remove sdb data table failed:csNames=" + tableNames,
                    e);
        }
        finally {
            service.releaseSequoiadb(db);
        }
    }
}
