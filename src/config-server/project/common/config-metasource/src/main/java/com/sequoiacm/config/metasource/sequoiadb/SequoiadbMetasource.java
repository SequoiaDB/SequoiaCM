package com.sequoiacm.config.metasource.sequoiadb;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.metasource.MetasourceType;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.datasource.SequoiadbDatasource;

@Component
public class SequoiadbMetasource implements Metasource {

    private SequoiadbDatasource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(SequoiadbMetasource.class);

    @Autowired
    public SequoiadbMetasource(SdbMetasourceConfig config) throws Exception {
        SdbConfig sdbConnConfig = config.getSdbConfig();

        String user = sdbConnConfig.getUsername();
        AuthInfo auth = ScmFilePasswordParser.parserFile(sdbConnConfig.getPassword());
        List<String> urlList = sdbConnConfig.getUrls();

        ConfigOptions nwOpt = new ConfigOptions();
        nwOpt.setConnectTimeout(sdbConnConfig.getConnectTimeout());
        nwOpt.setMaxAutoConnectRetryTime(sdbConnConfig.getMaxAutoConnectRetryTime());
        nwOpt.setSocketKeepAlive(true);
        nwOpt.setSocketTimeout(sdbConnConfig.getSocketTimeout());
        nwOpt.setUseNagle(sdbConnConfig.getUseNagle());
        nwOpt.setUseSSL(sdbConnConfig.getUseSSL());

        DatasourceOptions dsOpt = new DatasourceOptions();
        dsOpt.setMaxCount(sdbConnConfig.getMaxConnectionNum());
        dsOpt.setDeltaIncCount(sdbConnConfig.getDeltaIncCount());
        dsOpt.setMaxIdleCount(sdbConnConfig.getMaxIdleNum());
        dsOpt.setKeepAliveTimeout(sdbConnConfig.getKeepAliveTime());
        dsOpt.setCheckInterval(sdbConnConfig.getRecheckCyclePeriod());
        dsOpt.setValidateConnection(sdbConnConfig.getValidateConnection());
        List<String> preferedInstance = new ArrayList<>();
        preferedInstance.add("M");
        dsOpt.setPreferedInstance(preferedInstance);
        dataSource = new SequoiadbDatasource(urlList, user, auth.getPassword(), nwOpt, dsOpt);
    }

    public void releaseConnection(Sequoiadb db) {
        try {
            if (db != null) {
                dataSource.releaseConnection(db);
            }
        }
        catch (Exception e) {
            logger.warn("failed release connection", e);
        }
    }

    public Sequoiadb getConnection() throws MetasourceException {
        Sequoiadb db = null;
        try {
            db = dataSource.getConnection();
            return db;
        }
        catch (Exception e) {
            throw new MetasourceException("failed to get connection", e);
        }
    }

    @Override
    public Transaction createTransaction() throws MetasourceException {
        return new SequoiadbTransaction(this);
    }

    @Override
    public MetasourceType getType() {
        return MetasourceType.SEQUOIADB;
    }

    @Override
    public TableDao getConfVersionTableDao() throws MetasourceException {
        return new SequoiadbTableDao(this, MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_CONF_VERSION);
    }

    @Override
    public TableDao getConfVersionTableDao(Transaction transaction) throws MetasourceException {
        return new SequoiadbTableDao(transaction, MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_CONF_VERSION);
    }

    @Override
    public TableDao getSubscribersTable() throws MetasourceException {
        return new SequoiadbTableDao(this, MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_SUBSCRIBERS);
    }

    public TableDao getCollection(String csName, String clName) {
        return new SequoiadbTableDao(this, csName, clName);
    }

    public TableDao getCollection(Transaction transaction, String csName, String clName) {
        return new SequoiadbTableDao(transaction, csName, clName);
    }

}
