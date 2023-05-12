package com.sequoiacm.infrastructure.audit;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.sequoiadb.base.UserConfig;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BSONTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.infrastructure.crypto.ScmFilePasswordParser;
import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;
import com.sequoiadb.datasource.SequoiadbDatasource;
import com.sequoiadb.datasource.DatasourceOptions;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;
import com.sequoiadb.base.ConfigOptions;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class ScmAuditSequoiadbAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ScmAuditSequoiadbAppender.class);

    private String url;
    private String userName;
    private String password;

    private String connectTimeout;
    private String maxAutoConnectRetryTime;
    private String socketTimeout;
    private String useNagle;
    private String useSsl;
    private String maxConnectionNum;
    private String deltaincCount;
    private String maxIdleNum;
    private String keepAliveTime;
    private String recheckCyclePeriod;
    private String validateConnection;
    private String location;
    private SequoiadbDatasource dataSource;
    private Date lastTime = new Date();
    private int count = 0;
    private Lock lock = new ReentrantLock();

    @Override
    protected void append(ILoggingEvent eventObject) {
        insertLog2SDB(eventObject);
    }

    /**
     * @param eventObject
     */
    private void insertLog2SDB(ILoggingEvent eventObject) {
        // 1 parse String auditLog msg

        BSONObject eventObj = new BasicBSONObject();
        BSONTimestamp stime = new BSONTimestamp(new Timestamp(eventObject.getTimeStamp()));
        String thread = eventObject.getThreadName();
        String level = eventObject.getLevel().toString();
        String message = eventObject.getMessage();

        eventObj = parseMSG(message);
        eventObj.put(ScmAuditDefine.AuditInfo.TIME, stime);
        eventObj.put(ScmAuditDefine.AuditInfo.THREAD, thread);
        eventObj.put(ScmAuditDefine.AuditInfo.LEVEL, level);
        // 2 getConnection() sdb
        Sequoiadb sdb = getConnection();
        if (null != sdb) {
            try {
                // get collectionSpace.collection : SCMSYSTEM.AUDIT_LOG_EVENT
                CollectionSpace scmAuditCS = sdb.getCollectionSpace(ScmAuditDefine.CS_AUDIT);
                DBCollection auditLogCL = scmAuditCS.getCollection(ScmAuditDefine.CL_AUDIT);
                insert(scmAuditCS, auditLogCL, eventObj);
            }
            catch (Exception e) {
                if (isCheckPoint()) {
                    logger.warn("auditLog to SDB : fail to get sequoiadb connection, url: " + url,
                            e);
                }
            }
            finally {
                releaseConnection(sdb);
            }
        }
        else {
            // 一个小时打印10条 ，不要获取不到链接一直在打日志
            // 判断：当前时间比lastTime大1小时 || count计数器小于10
            if (isCheckPoint()) {
                logger.warn("auditLog to SDB : fail to get sequoiadb connection, url: " + url);
            }
        }
    }

    private void insert(CollectionSpace scmAuditCS, DBCollection auditLogCL, BSONObject eventObj) {
        try {
            auditLogCL.insert(eventObj);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_CAT_NO_MATCH_CATALOG.getErrorCode()) {
                try {
                    attachSubCL(scmAuditCS, auditLogCL);
                    auditLogCL.insert(eventObj);
                }
                catch (Exception e1) {
                    if (isCheckPoint()) {
                        logger.warn(
                                "auditLog to SDB : fail to get sequoiadb connection, url: " + url,
                                e1);
                    }
                }
            }
        }
        logger.debug("Thread " + Thread.currentThread().getName() + "get audit info :"
                + eventObj.toString());
    }

    private void attachSubCL(CollectionSpace scmAuditCS, DBCollection auditLogCL) throws Exception {
        Date date = new Date();
        String currentYearMonth = DateCommonHelper.getCurrentYearMonth(date);
        String nextYearMonth = DateCommonHelper.getNextYearMonth(date);
        String collectionName = ScmAuditDefine.CL_AUDIT + "_" + currentYearMonth;
        String subClFullName = ScmAuditDefine.CS_AUDIT + "." + collectionName;
        try {
            BSONObject key = new BasicBSONObject(ScmAuditDefine.AuditInfo.TIME, 1);
            BSONObject options = new BasicBSONObject();
            options.put("ShardingType", "hash");
            options.put("ShardingKey", key);
            options.put("Compressed", true);
            options.put("CompressionType", "lzw");
            options.put("ReplSize", -1);
            options.put("AutoSplit", true);
            scmAuditCS.createCollection(collectionName, options);

            BSONObject attachOptions = new BasicBSONObject();
            BSONObject low = new BasicBSONObject();
            BSONObject upper = new BasicBSONObject();

            BSONTimestamp lowb = new BSONTimestamp(DateCommonHelper.getDate(currentYearMonth));
            BSONTimestamp upperb = new BSONTimestamp(DateCommonHelper.getDate(nextYearMonth));
            low.put(ScmAuditDefine.AuditInfo.TIME, lowb);
            upper.put(ScmAuditDefine.AuditInfo.TIME, upperb);
            attachOptions.put("LowBound", low);
            attachOptions.put("UpBound", upper);
            auditLogCL.attachCollection(subClFullName, attachOptions);
        }
        catch (Exception e) {
            logger.error("failed to attach sub cl = " + subClFullName, e);
            throw e;
        }
    }

    private boolean isCheckPoint() {
        Date now = new Date();
        long check = (now.getTime() - lastTime.getTime());
        if (check < ScmAuditDefine.CheckPoint.TIME) {
            if (count < 10) {
                count++;
                return true;
            }
            else {
                return false;
            }
        }
        else {
            lastTime = now;
            count = 0;
            return true;
        }
    }

    private BSONObject parseMSG(String message) {

        BSONObject eventObj = new BasicBSONObject();
        if ((null != message) && (!"".equals(message))) {
            String[] str = message.split(",", 8);
            String host = parseValue(str[0].split(":")[1]);
            String port = parseValue(str[1].split(":")[1]);
            String type = parseValue(str[2].split(":")[1]);
            String userType = parseValue(str[3].split(":")[1]);
            String user = parseValue(str[4].split(":")[1]);
            String ws = parseValue(str[5].split(":")[1]);
            String flag = parseValue(str[6].split(":")[1]);
            eventObj.put(ScmAuditDefine.AuditInfo.HOST, host);
            eventObj.put(ScmAuditDefine.AuditInfo.PORT, port);
            eventObj.put(ScmAuditDefine.AuditInfo.TYPE, type);
            eventObj.put(ScmAuditDefine.AuditInfo.USER_TYPE, userType);
            eventObj.put(ScmAuditDefine.AuditInfo.USER_NAME, user);
            eventObj.put(ScmAuditDefine.AuditInfo.WORK_SPACE, ws);
            eventObj.put(ScmAuditDefine.AuditInfo.FLAG, flag);
            String msg = parseValue(str[7]);
            eventObj.put(ScmAuditDefine.AuditInfo.MESSAGE, msg);
        }
        return eventObj;
    }

    private String parseValue(String value) {
        return value == null ? "" : value;
    }

    private Sequoiadb getConnection() {
        Sequoiadb sdb = null;

        if (null == dataSource) {
            try {
                lock.lock();
                if (null == dataSource) {
                    initDataSource();
                }
            }
            catch (Exception e) {
                logger.warn("auditLog to SDB : failed to init datasource", e);
            }
            finally {
                lock.unlock();
            }
        }
        try {
            sdb = dataSource.getConnection();
        }
        catch (Exception e) {
            if (null != sdb) {
                releaseConnection(sdb);
                sdb = null;
            }
            logger.warn("auditLog to SDB : failed to get sdb connection", e);
        }

        return sdb;
    }

    private void initDataSource() throws Exception {

        List<String> urlList = new ArrayList<String>();
        String[] urlArray = url.split(",");
        if (null != urlArray && urlArray.length > 0) {
            for (int i = 0; i < urlArray.length; i++) {
                urlList.add(urlArray[i].trim());
            }
        }

        ConfigOptions connConf = buildConfigOptions();
        DatasourceOptions datasourceConf = buildDatasourceOptions();
        String location = getLocation() == null ? "" : getLocation().trim();
        dataSource = SequoiadbDatasource.builder().serverAddress(urlList)
                .userConfig(new UserConfig(userName, password)).configOptions(connConf)
                .datasourceOptions(datasourceConf).location(location).build();
    }

    private DatasourceOptions buildDatasourceOptions() {
        DatasourceOptions datasourceConf = new DatasourceOptions();
        datasourceConf.setMaxCount(Integer.parseInt(maxConnectionNum));
        datasourceConf.setDeltaIncCount(Integer.parseInt(deltaincCount));
        datasourceConf.setMaxIdleCount(Integer.parseInt(maxIdleNum));
        datasourceConf.setKeepAliveTimeout(Integer.parseInt(keepAliveTime));
        datasourceConf.setCheckInterval(Integer.parseInt(recheckCyclePeriod));
        datasourceConf.setValidateConnection(Boolean.parseBoolean(validateConnection));
        return datasourceConf;
    }

    private ConfigOptions buildConfigOptions() {
        ConfigOptions connConf = new ConfigOptions();
        connConf.setConnectTimeout(Integer.parseInt(connectTimeout));
        connConf.setMaxAutoConnectRetryTime(Integer.parseInt(maxAutoConnectRetryTime));
        connConf.setSocketTimeout(Integer.parseInt(socketTimeout));
        connConf.setUseNagle(Boolean.parseBoolean(useNagle));
        connConf.setUseSSL(Boolean.parseBoolean(useSsl));
        return connConf;
    }

    private void releaseConnection(Sequoiadb sdb) {
        try {
            if (null != sdb) {
                dataSource.releaseConnection(sdb);
            }
        }
        catch (Exception e) {
            logger.warn("auditLog to SDB : release connection failed", e);
            try {
                sdb.close();
            }
            catch (Exception e1) {
                logger.warn("auditLog to SDB : disconnect sequoiadb failed", e1);
            }
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = ScmFilePasswordParser.parserFile(password).getPassword();
    }

    public String getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(String connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getMaxAutoConnectRetryTime() {
        return maxAutoConnectRetryTime;
    }

    public void setMaxAutoConnectRetryTime(String maxAutoConnectRetryTime) {
        this.maxAutoConnectRetryTime = maxAutoConnectRetryTime;
    }

    public String getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(String socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public String getUseNagle() {
        return useNagle;
    }

    public void setUseNagle(String useNagle) {
        this.useNagle = useNagle;
    }

    public String getUseSsl() {
        return useSsl;
    }

    public void setUseSsl(String useSsl) {
        this.useSsl = useSsl;
    }

    public String getMaxConnectionNum() {
        return maxConnectionNum;
    }

    public void setMaxConnectionNum(String maxConnectionNum) {
        this.maxConnectionNum = maxConnectionNum;
    }

    public String getDeltaincCount() {
        return deltaincCount;
    }

    public void setDeltaincCount(String deltaincCount) {
        this.deltaincCount = deltaincCount;
    }

    public String getMaxIdleNum() {
        return maxIdleNum;
    }

    public void setMaxIdleNum(String maxIdleNum) {
        this.maxIdleNum = maxIdleNum;
    }

    public String getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(String keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public String getValidateConnection() {
        return validateConnection;
    }

    public void setValidateConnection(String validateConnection) {
        this.validateConnection = validateConnection;
    }

    public String getRecheckCyclePeriod() {
        return recheckCyclePeriod;
    }

    public void setRecheckCyclePeriod(String recheckCyclePeriod) {
        this.recheckCyclePeriod = recheckCyclePeriod;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
