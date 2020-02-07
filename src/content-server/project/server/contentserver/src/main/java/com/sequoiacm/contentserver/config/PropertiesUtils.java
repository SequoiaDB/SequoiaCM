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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.PropertiesDefine;
import com.sequoiacm.contentserver.exception.ScmServerException;
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
        appProperties.add(PropertiesDefine.PROPERTY_ZK_CLIENTNUM);
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
                return getRootSiteUrl();
            case PropertiesDefine.PROPERTY_ROOTSITE_USER:
                return getRootSiteUser();
            case PropertiesDefine.PROPERTY_ROOTSITE_PASSWD:
                return getRootSitePassword();
            // sdb
            case PropertiesDefine.PROPERTY_SDB_CONNECTTIMEOUT:
                return String.valueOf(getConnectTimeout());
            case PropertiesDefine.PROPERTY_SDB_SOCKETTIMEOUT:
                return String.valueOf(getSocketTimeout());
            case PropertiesDefine.PROPERTY_SDB_MAXCONNECTRETRYTIME:
                return String.valueOf(getMaxConnectionNum());
            case PropertiesDefine.PROPERTY_SDB_USENAGLE:
                return String.valueOf(getUseNagleFlag());
            case PropertiesDefine.PROPERTY_SDB_USESSL:
                return String.valueOf(getUseSSLFlag());
            case PropertiesDefine.PROPERTY_SDB_KEEPALIVETIME:
                return String.valueOf(getSdbKeepAliveTime());
            case PropertiesDefine.PROPERTY_SDB_MAXCONNECTIONNUM:
                return String.valueOf(getMaxConnectionNum());
            case PropertiesDefine.PROPERTY_SDB_VALIDATECONNECTION:
                return String.valueOf(getValidateConnection());
            case PropertiesDefine.PROPERTY_SDB_DELTAINCCOUNT:
                return String.valueOf(getDeltaIncCount());
            case PropertiesDefine.PROPERTY_SDB_MAXIDLENUM:
                return String.valueOf(getMaxIdleNum());
            case PropertiesDefine.PROPERTY_SDB_RECHECKPERIOD:
                return String.valueOf(getRecheckCyclePeriod());

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
            case PropertiesDefine.PROPERTY_ZK_CLIENTNUM:
                return String.valueOf(getZkClientNum());
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
    public static String getZKConnUrl() {
        return zkConfig.getUrls();
    }

    public static int getZkClientNum() {
        return zkConfig.getClientNum();
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

    public static int getClenaJobChildThreshold() {
        return zkConfig.getClenaJobChildThreshold();
    }

    public static int getClenaJobCountThreshold() {
        return zkConfig.getClenaJobCountThreshold();
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

}
