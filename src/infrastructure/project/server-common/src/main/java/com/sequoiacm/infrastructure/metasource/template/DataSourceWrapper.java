package com.sequoiacm.infrastructure.metasource.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

public class DataSourceWrapper {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceWrapper.class);

    private SequoiadbDatasource dataSource = null;

    private static DataSourceWrapper dataSourceWrapper = new DataSourceWrapper();

    public static DataSourceWrapper getInstance() {
        return dataSourceWrapper;
    }

    public void init(SequoiadbDatasource dataSource) {
        this.dataSource = dataSource;
    }

    public SequoiadbDatasource getSdbDataSource() throws BaseException {
        if (dataSource != null) {
            return dataSource;
        }
        throw new BaseException(SDBError.SDB_INTERRUPT, "datasource is null");
    }

    public Sequoiadb getConnection() throws BaseException {
        Sequoiadb sdb = null;
        try {
            return dataSource.getConnection();
        }
        catch (Exception e) {
            releaseConnection(sdb);
            sdb = null;
            throw new BaseException(SDBError.SDB_INTERRUPT, "datasource error", e);
        }
    }

    public void releaseConnection(Sequoiadb sdb) {
        try {
            if (null != sdb) {
                dataSource.releaseConnection(sdb);
            }
        }
        catch (Exception e) {
            logger.warn("release connection failed", e);
            try {
                sdb.close();
            }
            catch (Exception e1) {
                logger.warn("disconnect sequoiadb failed", e1);
            }
        }
    }

    public void clear() {
        try {
            if (null != dataSource) {
                dataSource.close();
                dataSource = null;
            }
        }
        catch (Exception e) {
            logger.warn("close sequoiadb data source failed", e);
        }
    }
}
