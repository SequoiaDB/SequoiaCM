package com.sequoiacm.tools.tag.common;

import com.sequoiacm.infrastructure.tool.exception.ScmBaseExitCode;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.datasource.SequoiadbDatasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class SequoiadbDataSourceWrapper {
    private static final Logger logger = LoggerFactory.getLogger(SequoiadbDataSourceWrapper.class);
    private static volatile SequoiadbDataSourceWrapper instance = new SequoiadbDataSourceWrapper();
    private SequoiadbDatasource ds;
    private List<String> sdbUrls;
    private String sdbUser;
    private String sdbPassword;

    private SequoiadbDataSourceWrapper() {
    }

    public static SequoiadbDataSourceWrapper getInstance() {
        return instance;
    }

    public void init(List<String> urls, String user, String password) throws ScmToolsException {
        this.sdbUrls = urls;
        this.sdbUser = user;
        this.sdbPassword = password;

        ConfigOptions cfOpt = new ConfigOptions();
        cfOpt.setConnectTimeout(UpgradeConfig.getInstance().getIntConf("sdb.connectTimeout",
                cfOpt.getConnectTimeout()));
        cfOpt.setSocketTimeout(UpgradeConfig.getInstance().getIntConf("sdb.socketTimeout",
                cfOpt.getSocketTimeout()));
        DatasourceOptions dsOpt = new DatasourceOptions();
        dsOpt.setPreferredInstance(Collections.singletonList("M"));
        dsOpt.setMaxCount(UpgradeConfig.getInstance().getIntConf("sdb.maxCount", 10));
        ds = new SequoiadbDatasource(urls, user, password, cfOpt, dsOpt);
    }


    public List<String> getSdbUrls() {
        return sdbUrls;
    }

    public String getSdbPassword() {
        return sdbPassword;
    }

    public String getSdbUser() {
        return sdbUser;
    }

    public void destroy() {
        if (ds != null) {
            ds.close();
        }
    }

    public Sequoiadb getConnection() throws ScmToolsException {
        try {
            return ds.getConnection();
        }
        catch (Exception e) {
            throw new ScmToolsException("get connection error", ScmBaseExitCode.SYSTEM_ERROR, e);
        }
    }

    public void releaseConnection(Sequoiadb sdb) {
        if (sdb != null) {
            try {
                ds.releaseConnection(sdb);
            }
            catch (Exception e) {
                logger.warn("failed to release connection", e);
            }
        }
    }
}
