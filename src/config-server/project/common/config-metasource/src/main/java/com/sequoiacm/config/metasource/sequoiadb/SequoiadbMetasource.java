package com.sequoiacm.config.metasource.sequoiadb;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.metasource.*;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.DBCursor;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.base.UserConfig;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.datasource.SequoiadbDatasource;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

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
        dsOpt.setPreferredInstance(preferedInstance);
        String location = sdbConnConfig.getLocation() == null ? ""
                : sdbConnConfig.getLocation().trim();
        dataSource = SequoiadbDatasource.builder().serverAddress(urlList)
                .userConfig(new UserConfig(user, auth.getPassword())).configOptions(nwOpt)
                .datasourceOptions(dsOpt).location(location).build();
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

    public void ensureIndex(String csName, String clName, String idxName,
            BSONObject indexDefinition, boolean isUnique) throws MetasourceException {
        Sequoiadb db = getConnection();
        try {
            CollectionSpace cs = db.getCollectionSpace(csName);
            DBCollection cl = cs.getCollection(clName);
            DBCursor cursor = cl.getIndex(idxName);
            if (cursor != null) {
                try {
                    if (cursor.hasNext()) {
                        cursor.close();
                        return;
                    }
                }
                finally {
                    cursor.close();
                }
            }

            cl.createIndex(idxName, indexDefinition, isUnique, false);

        }
        catch (BaseException e) {
            if (e.getErrorCode() != SDBError.SDB_IXM_EXIST.getErrorCode()
                    && e.getErrorCode() != SDBError.SDB_IXM_REDEF.getErrorCode()
                    && e.getErrorCode() != SDBError.SDB_IXM_EXIST_COVERD_ONE.getErrorCode()) {
                throw new MetasourceException("failed to create cl index:" + csName + "." + clName
                        + ", idxName=" + idxName + ", idxDef=" + indexDefinition + ", isUnique="
                        + isUnique, e);
            }
            logger.info("assume index exist, cl={}.{}, isUnique={}, indexDef={}, sdbError={},{}",
                    csName, clName, isUnique, indexDefinition, e.getErrorCode(), e.getMessage());
        }
        finally {
            releaseConnection(db);
        }
    }

    public void ensureCollection(String csName, String clName, BSONObject clOption)
            throws MetasourceException {
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
            throw new MetasourceException(
                    "failed to create cl:" + csName + "." + clName + ", option=" + clOption, e);
        }
        finally {
            releaseConnection(db);
        }
    }

    public void dropCollection(String csName, String clName, boolean skipRecycleBin)
            throws MetasourceException {
        Sequoiadb db = getConnection();
        try {
            CollectionSpace cs = db.getCollectionSpace(csName);
            BSONObject options = new BasicBSONObject("SkipRecycleBin", skipRecycleBin);
            cs.dropCollection(clName, options);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_NOTEXIST.getErrorCode()) {
                return;
            }
            throw new MetasourceException("failed to drop cl:" + csName + "." + clName, e);
        }
        finally {
            releaseConnection(db);
        }
    }

    @Override
    public Transaction createTransaction() throws MetasourceException {
        return new SequoiadbTransaction(this);
    }

    @Override
    public ScmGlobalConfigTableDao getScmGlobalConfigTableDao() throws MetasourceException {
        return new ScmGlobalConfigTableDaoImpl(this);
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

    public SequoiadbTableDao getCollection(String csName, String clName) {
        return new SequoiadbTableDao(this, csName, clName);
    }

    public SequoiadbTableDao getCollection(Transaction transaction, String csName, String clName) {
        return new SequoiadbTableDao(transaction, csName, clName);
    }

    @PostConstruct
    public void ensureTable() throws MetasourceException {
        ensureCollection(ScmGlobalConfigTableDaoImpl.CS_NAME, ScmGlobalConfigTableDaoImpl.CL_NAME,
                null);
        ensureIndex(ScmGlobalConfigTableDaoImpl.CS_NAME, ScmGlobalConfigTableDaoImpl.CL_NAME,
                "conf_name_idx", new BasicBSONObject(FieldName.GlobalConfig.FIELD_CONFIG_NAME, 1),
                true);
    }

}
