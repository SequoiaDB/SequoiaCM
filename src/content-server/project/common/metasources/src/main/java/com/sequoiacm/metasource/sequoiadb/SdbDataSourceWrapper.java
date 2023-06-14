package com.sequoiacm.metasource.sequoiadb;

import java.util.List;
import java.util.Map;

import com.sequoiacm.metasource.MetaSourceDefine;
import com.sequoiadb.base.UserConfig;
import com.sequoiadb.datasource.SequoiadbDatasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import com.sequoiadb.base.ConfigOptions;

public class SdbDataSourceWrapper {
    private static final Logger logger = LoggerFactory.getLogger(SdbDataSourceWrapper.class);

    SequoiadbDatasource dataSource = null;

    public SdbDataSourceWrapper(List<String> urlList, String user, String passwd,
            ConfigOptions connConf, DatasourceOptions datasourceConf, String location)
            throws SdbMetasourceException {
        try {
// 回退sdb驱动至349，不支持location：SEQUOIACM-1411
//            location = location == null ? "" : location;
//            dataSource = SequoiadbDatasource.builder().serverAddress(urlList)
//                    .userConfig(new UserConfig(user, passwd)).configOptions(connConf)
//                    .datasourceOptions(datasourceConf).location(location).build();
            dataSource = SequoiadbDatasource.builder().serverAddress(urlList)
                    .userConfig(new UserConfig(user, passwd)).configOptions(connConf)
                    .datasourceOptions(datasourceConf).build();
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

    protected com.sequoiadb.datasource.SequoiadbDatasource getDataSource() {
        return dataSource;
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
