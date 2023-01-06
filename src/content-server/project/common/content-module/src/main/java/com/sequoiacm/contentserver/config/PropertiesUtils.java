/**
 * FileName: PropertiesUtils.java Copyright(C) 2016- SequoiaDB Ltd.
 *
 * Change Activity: defect Date Who Description ======= ============= ======
 * ========================= create 2017/1/4 ludejin
 *
 * Last Changed =
 */
package com.sequoiacm.contentserver.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sequoiacm.infrastructure.common.ZkAcl;
import com.sequoiacm.infrastructure.lock.ScmLockConfig;
import com.sequoiadb.datasource.ConnectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.PropertiesDefine;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.infrastructure.audit.ScmAuditConfig;

/**
 * The Class to get properties.
 *
 * @since SCM2.0
 */
@Component
public class PropertiesUtils {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtils.class);

    private static String jarDir = null;

    private static Set<String> appProperties = new HashSet<>();
    private static Map<String, String> internalProperties = new HashMap<>();

    private static RootSiteMetaConfig rootSiteMetaConfig;
    private static SdbConfig sdbConfig;
    private static ServerConfig serverConfig;
    private static ZkConfig zkConfig;
    private static PrivilegeHeartBeatConfig privilegeHeartBeatConfig;
    private static ScmAuditConfig auditConfig;
    private static ConfVersionConfig versionConfig;
    private static DirCacheConfig dirCacheConfig;
    private static CephS3DatasourceConfig cephS3Config;
    private static SftpDatasourceConfig sftpDatasourceConfig;

    @Autowired
    public void setRootSiteMetaConfig(RootSiteMetaConfig rootSiteMetaConfig) {
        PropertiesUtils.rootSiteMetaConfig = rootSiteMetaConfig;
    }

    @Autowired
    public void setSdbConfig(SdbConfig sdbConfig) {
        PropertiesUtils.sdbConfig = sdbConfig;
    }

    @Autowired
    public void setServerConfig(ServerConfig serverConfig) {
        PropertiesUtils.serverConfig = serverConfig;
    }

    @Autowired
    public void setZkConfig(ZkConfig zkConfig) {
        PropertiesUtils.zkConfig = zkConfig;
    }

    @Autowired
    public void setPrivilegeHeartBeatConfig(PrivilegeHeartBeatConfig privilegeHeartBeatConfig) {
        PropertiesUtils.privilegeHeartBeatConfig = privilegeHeartBeatConfig;
    }

    @Autowired
    public void setAuditConfig(ScmAuditConfig auditConfig) {
        PropertiesUtils.auditConfig = auditConfig;
    }

    @Autowired
    public void setDirCacheConfig(DirCacheConfig dirCacheConfig) {
        PropertiesUtils.dirCacheConfig = dirCacheConfig;
    }

    @Autowired
    public void setConfVersionConfig(ConfVersionConfig versionConfig) {
        PropertiesUtils.versionConfig = versionConfig;
    }

    @Autowired
    public void setCephS3Config(CephS3DatasourceConfig conf) {
        PropertiesUtils.cephS3Config = conf;
    }

    @Autowired
    public void setSftpDatasourceConfig(SftpDatasourceConfig conf) {
        PropertiesUtils.sftpDatasourceConfig = conf;
    }


    // 装载一些配置的key到特定集合中，对外暴露了通过key获取配置项的能力
    public static void loadSysConfig() throws ScmServerException {
        loadAppProperties();
    }

    public static String getProperty(String key) {
        if (appProperties.contains(key)) {
            return getAppProperty(key);
        }
        else if (internalProperties.containsKey(key)) {
            return internalProperties.get(key);
        }
        else {
            return null;
        }
    }

    public static Object setInternalProperty(String key, String value) {
        return internalProperties.put(key, value);
    }

    private static void loadAppProperties() {
        // rootSite
        appProperties.add(PropertiesDefine.PROPERTY_ROOTSITE_URL);
        appProperties.add(PropertiesDefine.PROPERTY_ROOTSITE_USER);
        appProperties.add(PropertiesDefine.PROPERTY_ROOTSITE_PASSWD);

        // rootSite new
        appProperties.add(PropertiesDefine.PROPERTY_ROOTSITE_URL_NEW);
        appProperties.add(PropertiesDefine.PROPERTY_ROOTSITE_USER_NEW);
        appProperties.add(PropertiesDefine.PROPERTY_ROOTSITE_PASSWD_NEW);

        // sdb
        appProperties.add(PropertiesDefine.PROPERTY_SDB_CONNECTTIMEOUT);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_SOCKETTIMEOUT);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_MAXCONNECTRETRYTIME);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_USENAGLE);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_USESSL);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_KEEPALIVETIME);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_MAXCONNECTIONNUM);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_VALIDATECONNECTION);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_DELTAINCCOUNT);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_MAXIDLENUM);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_RECHECKPERIOD);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_CONNECTSTRATEGY);

        // sdb new
        appProperties.add(PropertiesDefine.PROPERTY_SDB_CONNECTTIMEOUT_NEW);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_SOCKETTIMEOUT_NEW);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_MAXCONNECTRETRYTIME_NEW);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_USENAGLE_NEW);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_USESSL_NEW);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_KEEPALIVETIME_NEW);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_MAXCONNECTIONNUM_NEW);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_VALIDATECONNECTION_NEW);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_DELTAINCCOUNT_NEW);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_MAXIDLENUM_NEW);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_RECHECKPERIOD_NEW);
        appProperties.add(PropertiesDefine.PROPERTY_SDB_CONNECTSTRATEGY_NEW);

        // server
        appProperties.add(PropertiesDefine.PROPERTY_SERVER_PORT);
        appProperties.add(PropertiesDefine.PROPERTY_SERVER_TRANSFER_CHECK_LENGTH);

        // audit
        appProperties.add(PropertiesDefine.PROPERTY_AUDIT_MASK);
        appProperties.add(PropertiesDefine.PROPERTY_AUDIT_USERMASK);
        appProperties.add(PropertiesDefine.PROPERTY_AUDIT_USER);
        appProperties.add(PropertiesDefine.PROPERTY_AUDIT_USERTYPE);

        // zk
        appProperties.add(PropertiesDefine.PROPERTY_ZK_URL);
        appProperties.add(PropertiesDefine.PROPERTY_ZK_LOCKTIMEOUT);
        appProperties.add(PropertiesDefine.PROPERTI_ZK_CLIENT_TIMEOUT);

        // privilege
        appProperties.add(PropertiesDefine.PROPERTY_PRIVILEGE_HBINTERVAL);

        // directory cache
        appProperties.add(PropertiesDefine.PROPERTY_DIR_CACHE_ENABLE);
        appProperties.add(PropertiesDefine.PROPERTY_DIR_CACHE_MAXSIZE);
    }

    private static String getAppProperty(String key) {
        switch (key) {
            case PropertiesDefine.PROPERTY_ROOTSITE_URL:
            case PropertiesDefine.PROPERTY_ROOTSITE_URL_NEW:
                return getRootSiteUrl();
            case PropertiesDefine.PROPERTY_ROOTSITE_USER:
            case PropertiesDefine.PROPERTY_ROOTSITE_USER_NEW:
                return getRootSiteUser();
            case PropertiesDefine.PROPERTY_ROOTSITE_PASSWD:
            case PropertiesDefine.PROPERTY_ROOTSITE_PASSWD_NEW:
                return getRootSitePassword();
            // sdb
            case PropertiesDefine.PROPERTY_SDB_CONNECTTIMEOUT:
            case PropertiesDefine.PROPERTY_SDB_CONNECTTIMEOUT_NEW:
                return String.valueOf(getConnectTimeout());
            case PropertiesDefine.PROPERTY_SDB_SOCKETTIMEOUT:
            case PropertiesDefine.PROPERTY_SDB_SOCKETTIMEOUT_NEW:
                return String.valueOf(getSocketTimeout());
            case PropertiesDefine.PROPERTY_SDB_MAXCONNECTRETRYTIME:
            case PropertiesDefine.PROPERTY_SDB_MAXCONNECTRETRYTIME_NEW:
                return String.valueOf(getMaxConnectionNum());
            case PropertiesDefine.PROPERTY_SDB_USENAGLE:
            case PropertiesDefine.PROPERTY_SDB_USENAGLE_NEW:
                return String.valueOf(getUseNagleFlag());
            case PropertiesDefine.PROPERTY_SDB_USESSL:
            case PropertiesDefine.PROPERTY_SDB_USESSL_NEW:
                return String.valueOf(getUseSSLFlag());
            case PropertiesDefine.PROPERTY_SDB_KEEPALIVETIME:
            case PropertiesDefine.PROPERTY_SDB_KEEPALIVETIME_NEW:
                return String.valueOf(getSdbKeepAliveTime());
            case PropertiesDefine.PROPERTY_SDB_MAXCONNECTIONNUM:
            case PropertiesDefine.PROPERTY_SDB_MAXCONNECTIONNUM_NEW:
                return String.valueOf(getMaxConnectionNum());
            case PropertiesDefine.PROPERTY_SDB_VALIDATECONNECTION:
            case PropertiesDefine.PROPERTY_SDB_VALIDATECONNECTION_NEW:
                return String.valueOf(getValidateConnection());
            case PropertiesDefine.PROPERTY_SDB_DELTAINCCOUNT:
            case PropertiesDefine.PROPERTY_SDB_DELTAINCCOUNT_NEW:
                return String.valueOf(getDeltaIncCount());
            case PropertiesDefine.PROPERTY_SDB_MAXIDLENUM:
            case PropertiesDefine.PROPERTY_SDB_MAXIDLENUM_NEW:
                return String.valueOf(getMaxIdleNum());
            case PropertiesDefine.PROPERTY_SDB_RECHECKPERIOD:
            case PropertiesDefine.PROPERTY_SDB_RECHECKPERIOD_NEW:
                return String.valueOf(getRecheckCyclePeriod());
            case PropertiesDefine.PROPERTY_SDB_CONNECTSTRATEGY:
            case PropertiesDefine.PROPERTY_SDB_CONNECTSTRATEGY_NEW:
                return String.valueOf(getConnectStrategy());
            // server
            case PropertiesDefine.PROPERTY_SERVER_PORT:
                return String.valueOf(getServerPort());
            case PropertiesDefine.PROPERTY_SERVER_TRANSFER_CHECK_LENGTH:
                return String.valueOf(getTransferCheckLength());
            case PropertiesDefine.PROPERTY_SERVER_TRANSFER_CONNECT_TIMEOUT:
                return String.valueOf(getTransferConnectTimeout());
            case PropertiesDefine.PROPERTY_SERVER_TRANSFER_READ_TIMEOUT:
                return String.valueOf(getTransferReadTimeout());
            case PropertiesDefine.PROPERTY_SERVER_LIST_INSTANCE_CHECK_INTERVAL:
                return String.valueOf(getListInstanceCheckInterval());

            // audit
            case PropertiesDefine.PROPERTY_AUDIT_MASK:
                return getAuditMask();
            case PropertiesDefine.PROPERTY_AUDIT_USERMASK:
                return getUserAuditMask();
            case PropertiesDefine.PROPERTY_AUDIT_USER:
                return getUserAuditType();
            case PropertiesDefine.PROPERTY_AUDIT_USERTYPE:
                return getUserTypeAuditType();

            // zk
            case PropertiesDefine.PROPERTY_ZK_URL:
                return getZKConnUrl();
            case PropertiesDefine.PROPERTY_ZK_LOCKTIMEOUT:
                return String.valueOf(getZkLockTimeout());
            case PropertiesDefine.PROPERTI_ZK_CLIENT_TIMEOUT:
                return String.valueOf(getZkClientTimeout());

            // privilege
            case PropertiesDefine.PROPERTY_PRIVILEGE_HBINTERVAL:
                return String.valueOf(getPrivilegeHeartBeatInterval());

            // directory cache
            case PropertiesDefine.PROPERTY_DIR_CACHE_ENABLE:
                return String.valueOf(enableDirCache());
            case PropertiesDefine.PROPERTY_DIR_CACHE_MAXSIZE:
                return String.valueOf(getDirCacheMaxSize());
        }

        return null;
    }

    // *********************** ROOT SITE ******************************
    public static String getRootSiteUrl() {
        return rootSiteMetaConfig.getUrl();
    }

    public static String getRootSiteUser() {
        return rootSiteMetaConfig.getUser();
    }

    public static String getRootSitePassword() {
        return rootSiteMetaConfig.getPassword();
    }

    // *********************** SEQUOIADB ******************************
    public static int getConnectTimeout() {
        return sdbConfig.getConnectTimeout();
    }

    public static int getSocketTimeout() {
        return sdbConfig.getSocketTimeout();
    }

    public static long getAutoConnectRetryTime() {
        return sdbConfig.getMaxAutoConnectRetryTime();
    }

    public static boolean getUseNagleFlag() {
        return sdbConfig.getUseNagle();
    }

    public static boolean getUseSSLFlag() {
        return sdbConfig.getUseSSL();
    }

    public static int getSdbKeepAliveTime() {
        return sdbConfig.getKeepAliveTime();
    }

    public static int getMaxConnectionNum() {
        return sdbConfig.getMaxConnectionNum();
    }

    public static boolean getValidateConnection() {
        return sdbConfig.getValidateConnection();
    }

    public static int getDeltaIncCount() {
        return sdbConfig.getDeltaIncCount();
    }

    public static int getMaxIdleNum() {
        return sdbConfig.getMaxIdleNum();
    }

    public static int getRecheckCyclePeriod() {
        return sdbConfig.getRecheckCyclePeriod();
    }

    public static ConnectStrategy getConnectStrategy(){
        return sdbConfig.getConnectStrategy();
    }

    // *********************** SERVER ******************************
    public static int getServerPort() {
        return serverConfig.getServerPort();
    }

    public static int getTransferCheckLength() {
        return serverConfig.getTransferCheckLength();
    }

    public static int getTransferConnectTimeout() {
        return serverConfig.getTransferConnectTimeout();
    }

    public static int getTransferReadTimeout() {
        return serverConfig.getTransferReadTimeout();
    }

    public static int getListInstanceCheckInterval() {
        return serverConfig.getListInstanceCheckInterval();
    }

    // *********************** zoo keeper ******************************

    public static ScmLockConfig getScmLockConfig() {
        ScmLockConfig config = new ScmLockConfig();
        config.setUrls(getZKConnUrl());
        config.setAcl(getZKAcl());
        config.setCleanJobPeriod(getZKCleanJobPeriod());
        config.setCleanJobResidualTime(getZKCleanJobResidual());
        config.setMaxBuffer(getMaxBuffer());
        config.setMaxCleanThreads(getMaxCleanThreads());
        config.setCoreCleanThreads(getCoreCleanThreads());
        config.setCleanQueueSize(getCleanQueueSize());
        config.adjustCoreCleanThreads();
        return config;
    }
    public static String getZKConnUrl() {
        return zkConfig.getUrls();
    }

    public static ZkAcl getZKAcl() {
        return zkConfig.getAcl();
    }

    public static int getZkLockTimeout() {
        return zkConfig.getLockTimeout();
    }

    public static int getZkClientTimeout() {
        return zkConfig.getClientTimeout();
    }

    public static long getZKCleanJobPeriod() {
        return zkConfig.getCleanJobPeriod();
    }

    public static long getZKCleanJobResidual() {
        return zkConfig.getCleanJobResidualTime();
    }

    public static int getMaxCleanThreads() {
        return zkConfig.getMaxCleanThreads();
    }

    public static int getCoreCleanThreads() {
        return zkConfig.getCoreCleanThreads();
    }

    public static int getCleanQueueSize() {
        return zkConfig.getCleanQueueSize();
    }

    public static int getMaxBuffer() {
        return zkConfig.getMaxBuffer();
    }

    // *********************** privilege ******************************
    public static int getPrivilegeHeartBeatInterval() {
        return privilegeHeartBeatConfig.getInterval();
    }

    // *********************** audit ******************************
    public static String getAuditMask() {
        return auditConfig.getMask();
    }

    public static String getUserAuditMask() {
        return auditConfig.getUserMask();
    }

    public static String getUserAuditType() {
        return auditConfig.getUser().toString();
    }

    public static String getUserTypeAuditType() {
        return auditConfig.getUserType().toString();
    }

    public static void setJarPath(String jarDir) {
        PropertiesUtils.jarDir = jarDir;
    }

    public static String getJarPath() {
        return jarDir;
    }

    public static void logSysConf() {
        logger.info("properties:");
        for (String key : appProperties) {
            logger.info(key + "=" + getAppProperty(key));
        }
    }

    public static long getWorkspaceVersionHeartbeat() {
        return versionConfig.getWorkspaceHeartbeat();
    }

    public static long getSiteVersionHeartbeat() {
        return versionConfig.getSiteHeartbeat();
    }

    public static long getMetaDataVersionHearbeat() {
        return versionConfig.getMetaDataHeartbeat();
    }

    public static int getDirCacheMaxSize() {
        return dirCacheConfig.getMaxSize();
    }

    public static boolean enableDirCache() {
        return dirCacheConfig.isEnable();
    }

    public static long getNodeVersionHeartbeat() {
        return versionConfig.getNodeHeartbeat();
    }

    public static ServerConfig getServerConfig() {
        return serverConfig;
    }

    public static CephS3DatasourceConfig getCephS3Config() {
        return cephS3Config;
    }

    public static SftpDatasourceConfig getSftpDatasourceConfig() {
        return sftpDatasourceConfig;
    }

}