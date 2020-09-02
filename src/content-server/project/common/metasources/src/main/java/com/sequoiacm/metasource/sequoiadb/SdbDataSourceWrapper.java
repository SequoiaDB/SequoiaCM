package com.sequoiacm.metasource.sequoiadb;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.base.SequoiadbDatasource;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import com.sequoiadb.net.ConfigOptions;

public class SdbDataSourceWrapper {
    private static final Logger logger = LoggerFactory.getLogger(SdbDataSourceWrapper.class);
    private static IDataSourceHandler dataSourceHandler;

    SequoiadbDatasource dataSource = null;

    public static void setDataSourceHandler(IDataSourceHandler dataSourceHandler) {
        SdbDataSourceWrapper.dataSourceHandler = dataSourceHandler;
    }

    public SdbDataSourceWrapper(List<String> urlList, String user, String passwd,
            ConfigOptions connConf, DatasourceOptions datasourceConf)
            throws SdbMetasourceException {
        try {
            dataSource = new SequoiadbDatasource(urlList, user, passwd, connConf, datasourceConf);
            if (dataSourceHandler != null) {
                dataSourceHandler.refresh(dataSource);
            }
        }
        catch (BaseException e) {
            throw new SdbMetasourceException(e.getErrorCode(), "failed to init datasource", e);
        }
        catch (Exception e) {
            throw new SdbMetasourceException(SDBError.SDB_SYS.getErrorCode(),
                    "failed to init datasource", e);
        }
    }

    private void recordConnection(Sequoiadb sdb) {
        MetaSequoiadbRecorder.getInstance().record(sdb);
    }

    private void unrecordConnection(Sequoiadb sdb) {
        MetaSequoiadbRecorder.getInstance().unrecord(sdb);
    }

    public Sequoiadb getConnection() throws SdbMetasourceException {
        Sequoiadb sdb = null;
        try {
            sdb = dataSource.getConnection();
            recordConnection(sdb);
            return sdb;
        }
        catch (BaseException e) {
            if (null != sdb) {
                releaseConnection(sdb);
                sdb = null;
            }
            throw new SdbMetasourceException(e.getErrorCode(), "datasource error", e);
        }
        catch (Exception e) {
            if (null != sdb) {
                releaseConnection(sdb);
                sdb = null;
            }
            throw new SdbMetasourceException(SDBError.SDB_INTERRUPT.getErrorCode(), "interrupted",
                    e);
        }
    }

    public void releaseConnection(Sequoiadb sdb) {
        try {
            if (null != sdb) {
                unrecordConnection(sdb);
                dataSource.releaseConnection(sdb);
            }
        }
        catch (Exception e) {
            logger.warn("release connection failed", e);
            try {
                sdb.disconnect();
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
                if (dataSourceHandler != null) {
                    dataSourceHandler.clear();
                }
            }
        }
        catch (Exception e) {
            logger.warn("close sequoiadb data source failed", e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("idleNum=").append(dataSource.getIdleConnNum()).append(",");
        sb.append("UsedNum=").append(dataSource.getUsedConnNum()).append(",");
        sb.append("AbnormalAddrNum=").append(dataSource.getAbnormalAddrNum()).append(",");
        sb.append("NormalAddrNum=").append(dataSource.getNormalAddrNum()).append(",");

        return sb.toString();
    }
}
