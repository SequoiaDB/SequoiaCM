package com.sequoiacm.schedule.dao.sequoiadb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiacm.schedule.ScheduleApplicationConfig;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.base.SequoiadbDatasource;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.net.ConfigOptions;

@Repository
public class SdbDataSourceWrapper {
    private static final Logger logger = LoggerFactory.getLogger(SdbDataSourceWrapper.class);

    SequoiadbDatasource dataSource = null;

    @Autowired
    public SdbDataSourceWrapper(ScheduleApplicationConfig config) throws Exception {
        String user = config.getMetaUser();
        AuthInfo auth = ScmFilePasswordParser.parserFile(config.getMetaPassword());
        String[] urlArray = config.getMetaUrl().split(",");
        List<String> urlList = new ArrayList<>();
        Collections.addAll(urlList, urlArray);

        ConfigOptions nwOpt = new ConfigOptions();
        nwOpt.setConnectTimeout(config.getConnectTimeout());
        nwOpt.setMaxAutoConnectRetryTime(config.getMaxAutoConnectRetryTime());
        nwOpt.setSocketKeepAlive(true);
        nwOpt.setSocketTimeout(config.getSocketTimeout());
        nwOpt.setUseNagle(config.getUseNagle());
        nwOpt.setUseSSL(config.getUseSSL());

        DatasourceOptions dsOpt = new DatasourceOptions();
        dsOpt.setMaxCount(config.getMaxConnectionNum());
        dsOpt.setDeltaIncCount(config.getDeltaIncCount());
        dsOpt.setMaxIdleCount(config.getMaxIdleNum());
        dsOpt.setKeepAliveTimeout(config.getKeepAliveTime());
        dsOpt.setCheckInterval(config.getRecheckCyclePeriod());
        dsOpt.setValidateConnection(config.getValidateConnection());
        List<String> preferedInstance = new ArrayList<>();
        preferedInstance.add("M");
        dsOpt.setPreferedInstance(preferedInstance);
        dataSource = new SequoiadbDatasource(urlList, user, auth.getPassword(), nwOpt, dsOpt);
    }

    private void recordConnection(Sequoiadb sdb) {
        MetaSequoiadbRecorder.getInstance().record(sdb);
    }

    private void unrecordConnection(Sequoiadb sdb) {
        MetaSequoiadbRecorder.getInstance().unrecord(sdb);
    }

    public Sequoiadb getConnection() throws Exception {
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
            throw e;
        }
        catch (Exception e) {
            if (null != sdb) {
                releaseConnection(sdb);
                sdb = null;
            }
            throw e;
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
