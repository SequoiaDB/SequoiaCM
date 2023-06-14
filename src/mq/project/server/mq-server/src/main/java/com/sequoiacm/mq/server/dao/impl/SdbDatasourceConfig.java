package com.sequoiacm.mq.server.dao.impl;

import java.util.ArrayList;
import java.util.List;

import com.sequoiadb.base.UserConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequoiacm.infrastructure.crypto.AuthInfo;
import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.datasource.SequoiadbDatasource;

@Configuration
@ConfigurationProperties(prefix = "scm.store.sequoiadb")
public class SdbDatasourceConfig {
    private List<String> urls;
    private String username;
    private String password;

    private final ConfigOptions connConf = new ConfigOptions();
    private final DatasourceOptions dsConf = new DatasourceOptions();
    private int connectTimeout = connConf.getConnectTimeout();
    private int socketTimeout = connConf.getSocketTimeout();
    private long maxAutoConnectRetryTime = connConf.getMaxAutoConnectRetryTime();
    private boolean useNagle = connConf.getUseNagle();
    private boolean useSSL = connConf.getUseSSL();
    private int keepAliveTime = 60 * 1000;
    private int maxConnectionNum = dsConf.getMaxCount();
    private boolean validateConnection = true;
    private int deltaIncCount = dsConf.getDeltaIncCount();
    private int maxIdleNum = dsConf.getMaxIdleCount();
    private int recheckCyclePeriod = 30 * 1000;
    private String location;

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public long getMaxAutoConnectRetryTime() {
        return maxAutoConnectRetryTime;
    }

    public void setMaxAutoConnectRetryTime(long maxAutoConnectRetryTime) {
        this.maxAutoConnectRetryTime = maxAutoConnectRetryTime;
    }

    public boolean getUseNagle() {
        return useNagle;
    }

    public void setUseNagle(boolean useNagle) {
        this.useNagle = useNagle;
    }

    public boolean getUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public int getMaxConnectionNum() {
        return maxConnectionNum;
    }

    public void setMaxConnectionNum(int maxConnectionNum) {
        this.maxConnectionNum = maxConnectionNum;
    }

    public boolean getValidateConnection() {
        return validateConnection;
    }

    public void setValidateConnection(boolean validateConnection) {
        this.validateConnection = validateConnection;
    }

    public int getDeltaIncCount() {
        return deltaIncCount;
    }

    public void setDeltaIncCount(int deltaIncCount) {
        this.deltaIncCount = deltaIncCount;
    }

    public int getMaxIdleNum() {
        return maxIdleNum;
    }

    public void setMaxIdleNum(int maxIdleNum) {
        this.maxIdleNum = maxIdleNum;
    }

    public int getRecheckCyclePeriod() {
        return recheckCyclePeriod;
    }

    public void setRecheckCyclePeriod(int recheckCyclePeriod) {
        this.recheckCyclePeriod = recheckCyclePeriod;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Bean
    public SequoiadbDatasource sdbDatasource(SdbDatasourceConfig configuration) {
        ConfigOptions nwOpt = new ConfigOptions();
        nwOpt.setConnectTimeout(configuration.getConnectTimeout());
        nwOpt.setMaxAutoConnectRetryTime(configuration.getMaxAutoConnectRetryTime());
        nwOpt.setSocketKeepAlive(true);
        nwOpt.setSocketTimeout(configuration.getSocketTimeout());
        nwOpt.setUseNagle(configuration.getUseNagle());
        nwOpt.setUseSSL(configuration.getUseSSL());

        DatasourceOptions dsOpt = new DatasourceOptions();
        dsOpt.setMaxCount(configuration.getMaxConnectionNum());
        dsOpt.setDeltaIncCount(configuration.getDeltaIncCount());
        dsOpt.setMaxIdleCount(configuration.getMaxIdleNum());
        dsOpt.setKeepAliveTimeout(configuration.getKeepAliveTime());
        dsOpt.setCheckInterval(configuration.getRecheckCyclePeriod());
        dsOpt.setValidateConnection(configuration.getValidateConnection());
        List<String> preferedInstance = new ArrayList<>();
        preferedInstance.add("M");
        dsOpt.setPreferredInstance(preferedInstance);
        AuthInfo auth = ScmFilePasswordParser.parserFile(configuration.getPassword());
// 回退sdb驱动至349，不支持location：SEQUOIACM-1411
//        String location = configuration.getLocation() == null ? ""
//                : configuration.getLocation().trim();
//        return SequoiadbDatasource.builder().serverAddress(configuration.getUrls())
//                .userConfig(new UserConfig(configuration.getUsername(), auth.getPassword()))
//                .configOptions(nwOpt).datasourceOptions(dsOpt).location(location).build();
        return SequoiadbDatasource.builder().serverAddress(configuration.getUrls())
                .userConfig(new UserConfig(configuration.getUsername(), auth.getPassword()))
                .configOptions(nwOpt).datasourceOptions(dsOpt).build();
    }
}
