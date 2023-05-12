package com.sequoiacm.cloud.adminserver.metasource.sequoiadb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sequoiadb.base.UserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.cloud.adminserver.AdminServerConfig;
import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.cloud.adminserver.metasource.MetaAccessor;
import com.sequoiacm.cloud.adminserver.metasource.MetaSource;
import com.sequoiacm.cloud.adminserver.metasource.MetaSourceDefine;
import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.datasource.SequoiadbDatasource;

@Component
public class SequoiadbMetaSource implements MetaSource {

    private SequoiadbDatasource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(SequoiadbMetaSource.class);

    @Autowired
    public SequoiadbMetaSource(AdminServerConfig config) throws Exception {

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
        dsOpt.setPreferredInstance(preferedInstance);

        String location = config.getLocation() == null ? "" : config.getLocation().trim();
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

    public Sequoiadb getConnection() throws ScmMetasourceException {
        Sequoiadb db = null;
        try {
            db = dataSource.getConnection();
            return db;
        }
        catch (Exception e) {
            throw new ScmMetasourceException("failed to get connection", e);
        }
    }

    @Override
    public MetaAccessor getContentServerAccessor() throws ScmMetasourceException {
        return new SequoiadbMetaAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_CONTENTSERVER);
    }

    @Override
    public MetaAccessor getWorkspaceAccessor() throws ScmMetasourceException {
        return new SequoiadbMetaAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_WORKSPACE);
    }

    @Override
    public MetaAccessor getSiteAccessor() throws ScmMetasourceException {
        return new SequoiadbMetaAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_SITE);
    }

    @Override
    public MetaAccessor getTrafficAccessor() throws ScmMetasourceException {
        return new SequoiadbMetaAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_TRAFFIC);
    }

    @Override
    public MetaAccessor getFileDeltaAccessor() throws ScmMetasourceException {
        return new SequoiadbMetaAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_FILE_DELTA);
    }

    @Override
    public MetaAccessor getObjectDeltaAccessor() throws ScmMetasourceException {
        return new SequoiadbMetaAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_OBJECT_DELTA);
    }

    @Override
    public MetaAccessor getFileStatisticsAccessor() throws ScmMetasourceException {
        return new SequoiadbMetaAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_STATISTICS_DATA);
    }

    @Override
    public MetaAccessor getBreakpointFileStatisticsAccessor() throws ScmMetasourceException {
        return new SequoiadbMetaAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_STATISTICS_BREAKPOINT_FILE);
    }

    @Override
    public MetaAccessor getQuotaSyncAccessor() {
        return new SequoiadbMetaAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_QUOTA_SYNC);
    }

    @Override
    public MetaAccessor getQuotaConfigAccessor() {
        return new SequoiadbMetaAccessor(this, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_QUOTA_CONFIG);
    }

}
