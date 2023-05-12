package com.sequoiacm.sequoiadb.dataservice;

import java.util.List;

import com.sequoiadb.base.UserConfig;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.sequoiadb.SequoiadbException;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import com.sequoiadb.base.ConfigOptions;

public class DataSourceWrapper {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceWrapper.class);

    SequoiadbDatasource dataSource = null;

    public static final int SESSION_TARGET_PRIMARY = 0;
    public static final int SESSION_TARGET_ANYONE = 1;
    BSONObject sessionAnyoneAttr = new BasicBSONObject("PreferedInstance", "A");

    private int siteId = 0;

    public DataSourceWrapper(int siteId, List<String> urlList, String user, String passwd,
            ConfigOptions connConf, DatasourceOptions datasourceConf) throws SequoiadbException {
        try {
            this.siteId = siteId;
            dataSource = SequoiadbDatasource.builder().serverAddress(urlList)
                    .userConfig(new UserConfig(user, passwd)).configOptions(connConf)
                    .datasourceOptions(datasourceConf).location(SdbDatasourceConfig.getLocation())
                    .build();
        }
        catch (BaseException e) {
            throw new SequoiadbException(e.getErrorCode(), "failed to init datasource", e);
        }
    }

    private void recordConnection(Sequoiadb sdb) {
        SequoiadbRecorder.getInstance().record(siteId, sdb);
    }

    private void unrecordConnection(Sequoiadb sdb) {
        SequoiadbRecorder.getInstance().unrecord(siteId, sdb);
    }

    public Sequoiadb getConnection(int preferedInstanceType) throws SequoiadbException {
        Sequoiadb sdb = null;
        try {
            sdb = dataSource.getConnection();
            recordConnection(sdb);
            if (preferedInstanceType == SESSION_TARGET_ANYONE) {
                sdb.setSessionAttr(sessionAnyoneAttr);
            }
            return sdb;
        }
        catch (BaseException e) {
            if (null != sdb) {
                releaseConnection(sdb);
                sdb = null;
            }
            throw new SequoiadbException(e.getErrorCode(), "datasource error", e);
        }
        catch (Exception e) {
            if (null != sdb) {
                releaseConnection(sdb);
                sdb = null;
            }
            throw new SequoiadbException(SDBError.SDB_INTERRUPT.getErrorCode(), "interrupted", e);
        }
    }

    public Sequoiadb getConnection() throws SequoiadbException {
        return getConnection(SESSION_TARGET_PRIMARY);
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
