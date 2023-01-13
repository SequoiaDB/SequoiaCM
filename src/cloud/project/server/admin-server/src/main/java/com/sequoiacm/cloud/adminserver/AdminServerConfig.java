package com.sequoiacm.cloud.adminserver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.sequoiacm.cloud.adminserver.common.StatisticsDefine;
import com.sequoiadb.base.ConfigOptions;
import com.sequoiadb.datasource.DatasourceOptions;

@Component
@ConfigurationProperties(prefix = "scm.store.sequoiadb")
class SdbConfig {
    private String urls;
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


    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
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

}

@Component
@ConfigurationProperties(prefix = "scm.server")
class ServerConfig {
    private int listInstanceCheckInterval = 2000;

    @Value("${server.port}")
    private int serverPort;

    public int getListInstanceCheckInterval() {
        return listInstanceCheckInterval;
    }

    public void setListInstanceCheckInterval(int listInstanceCheckInterval) {
        this.listInstanceCheckInterval = listInstanceCheckInterval;
    }

    public int getServerPort() {
        return serverPort;
    }
}

@Component
@Configuration
public class AdminServerConfig {

    @Autowired
    private SdbConfig sdbConfig;

    @Autowired
    private ServerConfig serverConfig;

    @Value("${scm.statistics.job.firstTime:" + StatisticsDefine.DefaultValue.JOB_FIRST_TIME + "}")
    private String jobFirstTime;

    @Value("${scm.statistics.job.period:" + StatisticsDefine.DefaultValue.JOB_PERIOD + "}")
    private String jobPeriod;

    @Value("${scm.statistics.job.breakpointFileCleanPeriod:"
            + StatisticsDefine.DefaultValue.JOB_BREAKPOINT_FILE_CLEAN_PERIOD + "}")
    private String breakpointFileCleanPeriod;

    @Value("${scm.statistics.job.breakpointFileStayDays:"
            + StatisticsDefine.DefaultValue.JOB_BREAKPOINT_FILE_STAY_DAYS + "}")
    private String breakpointFileStayDays;

    public Date getJobFirstTime() throws Exception {
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss");
        Date parseDate = timeFmt.parse(jobFirstTime);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, parseDate.getHours());
        calendar.set(Calendar.MINUTE, parseDate.getMinutes());
        calendar.set(Calendar.SECOND, parseDate.getSeconds());
        return calendar.getTime();
    }

    public long getJobPeriod() {
        long oneDay = 24 * 60 * 60 * 1000;
        return Integer.parseInt(jobPeriod.replace("d", "")) * oneDay;
    }

    public long getBreakpointFileCleanPeriod() {
        long oneDay = 24 * 60 * 60 * 1000;
        return Long.parseLong(breakpointFileCleanPeriod.replace("d", "")) * oneDay;
    }

    public long getBreakpointFileStayDays() {
        return Long.parseLong(breakpointFileStayDays.replace("d", ""));
    }

    public int getConnectTimeout() {
        return sdbConfig.getConnectTimeout();
    }

    public int getSocketTimeout() {
        return sdbConfig.getSocketTimeout();
    }

    public long getMaxAutoConnectRetryTime() {
        return sdbConfig.getMaxAutoConnectRetryTime();
    }

    public boolean getUseNagle() {
        return sdbConfig.getUseNagle();
    }

    public boolean getUseSSL() {
        return sdbConfig.getUseSSL();
    }

    public int getKeepAliveTime() {
        return sdbConfig.getKeepAliveTime();
    }

    public int getMaxConnectionNum() {
        return sdbConfig.getMaxConnectionNum();
    }

    public boolean getValidateConnection() {
        return sdbConfig.getValidateConnection();
    }

    public int getDeltaIncCount() {
        return sdbConfig.getDeltaIncCount();
    }

    public int getMaxIdleNum() {
        return sdbConfig.getMaxIdleNum();
    }

    public int getRecheckCyclePeriod() {
        return sdbConfig.getRecheckCyclePeriod();
    }

    public SdbConfig getSdbConfig() {
        return sdbConfig;
    }

    public String getMetaUrl() {
        return sdbConfig.getUrls();
    }

    public String getMetaUser() {
        return sdbConfig.getUsername();
    }

    public String getMetaPassword() {
        return sdbConfig.getPassword();
    }

    public int getServerPort() {
        return serverConfig.getServerPort();
    }

    public int getListInstanceCheckInterval() {
        return serverConfig.getListInstanceCheckInterval();
    }
}
