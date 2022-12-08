package com.sequoiacm.client.core;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.dispatcher.RestDispatcher;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.ScmHelper;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class ScmPoolingSessionMgrImpl implements ScmSessionMgr {

    private static final Logger logger = LoggerFactory.getLogger(ScmPoolingSessionMgrImpl.class);

    private static final int ABNORMAL_SESSION_QUEUE_SIZE = 1000;
    private static final long HTTP_CLIENT_IDLE_CONNECTION_MONITOR_INTERVAL = 5000;
    private static final long REFRESH_NODE_GROUP_INTERVAL = 30000;
    private static final String AUTHORIZATION = "x-auth-token";
    private static final String HTTP_CLIENT_HOST_ATTRIBUTE = "http.target_host";
    public static final String NODE_GROUP = "nodeGroup";

    private ScmSessionQueue authSessionQueue;
    private ScmSessionQueue notAuthSessionQueue;
    private ScmSessionQueue abnormalSessionQueue;

    private ScmTimer syncGatewayAddrTaskTimer;
    private ScmTimer checkGatewayAddrTaskTimer;
    private ScmTimer clearAbnormalSessionsTaskTimer;
    private ScmTimer idleConnectionMonitorTimer;
    private ScmTimer refreshNodeGroupTimer;

    // Dummy value to associate with an Object in the outerSessions Map
    private static final Object PRESENT = new Object();
    private final Map<ScmSession, Object> outerSessions = new ConcurrentHashMap<ScmSession, Object>();


    private IdleConnectionMonitorTask idleConnectionMonitorTask;

    private Map<String, Long> lastAccessTimeMap = new ConcurrentHashMap<String, Long>();

    private Map<String, Boolean> gatewayHealthStatus = new ConcurrentHashMap<String, Boolean>();

    private Map<String, UrlInfo> gatewayUrlInfo = new ConcurrentHashMap<String, UrlInfo>();

    private final CloseableHttpClient httpClient;

    private final ScmSessionPoolConf sessionPoolConf;

    private final ReadWriteLock closedStateLock = new ReentrantReadWriteLock();

    private boolean closed = false;

    private ScmUrlConfig basicUrl;

    private ScmUrlConfig lastUsedUrlConfig;
    private List<ScmUrlConfig> lastSplitUrlConfigs;
    private volatile boolean nodeGroupInitialed;

    ScmPoolingSessionMgrImpl(ScmSessionPoolConf conf) throws ScmException {
        try {
            this.sessionPoolConf = conf;
            this.authSessionQueue = new ScmSessionQueue(conf.getMaxCacheSize());
            this.notAuthSessionQueue = new ScmSessionQueue(conf.getMaxCacheSize());
            this.abnormalSessionQueue = new ScmSessionQueue(ABNORMAL_SESSION_QUEUE_SIZE);
            this.basicUrl = conf.getSessionConfig().getUrlConfig();
            addToUrlInfo(this.basicUrl.getUrlInfo());
            this.httpClient = createHttpClient();
            startTask();
        }
        catch (ScmInvalidArgumentException e) {
            releaseResource();
            throw e;
        }
        catch (Exception e) {
            releaseResource();
            throw new ScmException(ScmError.SYSTEM_ERROR, "failed to init sessionMgr", e);
        }

    }

    private void startTask() throws ScmInvalidArgumentException {
        // start sync gateway urls task
        long interval = sessionPoolConf.getSynGatewayUrlsInterval();
        if (interval > 0) {
            SyncGatewayAddrTask syncGatewayAddrTask = new SyncGatewayAddrTask(this, this.basicUrl,
                    new SyncGatewayAddrCallback() {
                        public void onNewUrlSync(List<UrlInfo> newUrls) {
                            try {
                                addToUrlInfo(newUrls);
                                updateScmUrlConfig(newUrls);
                            }
                            catch (ScmInvalidArgumentException e) {
                                logger.warn("failed to update url config", e);
                            }
                        }
                    });
            syncGatewayAddrTaskTimer = ScmTimerFactory.createScmTimer();
            syncGatewayAddrTaskTimer.schedule(syncGatewayAddrTask, interval, interval);
        }

        // start check gateway urls task
        interval = sessionPoolConf.getCheckGatewayUrlsInterval();
        CheckGatewayUrlsTask checkGatewayUrlsTask = new CheckGatewayUrlsTask(httpClient,
                this, new CheckGatewayUrlsTask.UrlsProvider() {
                    @Override
                    public Set<String> getUrls() {
                        List<String> currentUrls = sessionPoolConf.getSessionConfig().getUrls();
                        Set<String> pureUrls = new HashSet<String>(currentUrls.size());
                        // host:ip/siteName => host:ip
                        for (String url : currentUrls) {
                            pureUrls.add(getPureUrl(url));
                        }
                        for (Map.Entry<String, Boolean> entry : gatewayHealthStatus.entrySet()) {
                            if (!entry.getValue()) {
                                pureUrls.add(getPureUrl(entry.getKey()));
                            }
                        }
                        return pureUrls;
                    }
                });
        checkGatewayAddrTaskTimer = ScmTimerFactory.createScmTimer();
        checkGatewayAddrTaskTimer.schedule(checkGatewayUrlsTask, interval, interval);

        // start clear abnormal session task
        interval = sessionPoolConf.getClearAbnormalSessionInterval();
        ClearAbnormalSessionsTask clearAbnormalSessionsTask = new ClearAbnormalSessionsTask(this);
        clearAbnormalSessionsTaskTimer = ScmTimerFactory.createScmTimer();
        clearAbnormalSessionsTaskTimer.schedule(clearAbnormalSessionsTask, interval, interval);

        // start refresh node group task
        if (sessionPoolConf.getNodeGroup() != null) {
            refreshNodeGroupTimer = ScmTimerFactory.createScmTimer();
            refreshNodeGroupTimer.schedule(new ScmTimerTask() {
                @Override
                public void run() {
                    try {
                        refreshUrlNodeGroup();
                    }
                    catch (ScmException e) {
                        logger.warn("failed to refresh node group", e);
                    }
                }
            }, REFRESH_NODE_GROUP_INTERVAL, REFRESH_NODE_GROUP_INTERVAL);
        }

        // start httpClient idleConnectionMonitor task
        idleConnectionMonitorTimer = ScmTimerFactory.createScmTimer();
        idleConnectionMonitorTimer.schedule(idleConnectionMonitorTask,
                HTTP_CLIENT_IDLE_CONNECTION_MONITOR_INTERVAL,
                HTTP_CLIENT_IDLE_CONNECTION_MONITOR_INTERVAL);

    }

    @Override
    public ScmSession getSession(SessionType sessionType) throws ScmException {
        beforeGetSession();
        Lock lock = closedStateLock.readLock();
        lock.lock();
        try {
            if (closed) {
                throw new ScmException(ScmError.OPERATION_UNSUPPORTED, "sessionMgr is closed");
            }
            ScmSession scmSession = null;
            for (;;) {
                scmSession = pollFirst(sessionType);
                if (scmSession == null) {
                    scmSession = createSession(sessionType);
                    outerSessions.put(scmSession, PRESENT);
                    return scmSession;
                }
                if (isAvailable(scmSession)) {
                    outerSessions.put(scmSession, PRESENT);
                    return scmSession;
                }
                else {
                    discardSession(scmSession);
                }
            }
        }
        finally {
            lock.unlock();
        }

    }

    @Override
    public ScmSession getSession() throws ScmException {
        return getSession(SessionType.AUTH_SESSION);
    }

    void releaseSession(ScmSession scmSession) throws ScmException {
        Lock lock = closedStateLock.readLock();
        lock.lock();
        try {
            outerSessions.remove(scmSession);
            if (closed) {
                logger.warn("sessionMgr is closed");
                if (!scmSession.isClosed()) {
                    closeSessionSilence(scmSession);
                }
                if (outerSessions.isEmpty()) {
                    releaseResource();
                }
                return;
            }
            if (sessionCount() >= sessionPoolConf.getMaxCacheSize()) {
                discardSession(scmSession);
                return;
            }
            if (!offerFirst(scmSession)) {
                discardSession(scmSession);
            }
        }
        finally {
            lock.unlock();
        }

    }

    private void discardSession(ScmSession scmSession) {
        logger.debug("discard session:" + scmSession);
        if (!abnormalSessionQueue.offerFirst(scmSession)) {
            closeSessionSilence(scmSession);
        }
    }

    private void closeSessionSilence(ScmSession scmSession) {
        logger.debug("close session:" + scmSession);
        try {
            ((ScmPoolingRestSessionImpl) scmSession).destroy(isHealth(scmSession));
        }
        catch (ScmException e) {
            // ignore exception
        }
        lastAccessTimeMap.remove(scmSession.getSessionId());

    }

    private ScmSession createSession(SessionType sessionType) throws ScmException {
        List<ScmUrlConfig> splitUrlConfigs = null;
        ScmUrlConfig currentUrlConfig = sessionPoolConf.getSessionConfig().getUrlConfig();
        if (lastUsedUrlConfig != currentUrlConfig || lastSplitUrlConfigs == null) {
            synchronized (this) {
                splitUrlConfigs = splitAndSortUrlConfig(currentUrlConfig);
                lastSplitUrlConfigs = splitUrlConfigs;
                lastUsedUrlConfig = currentUrlConfig;
            }
        }
        else {
            splitUrlConfigs = lastSplitUrlConfigs;
        }
        ScmException lastException = null;
        for (ScmUrlConfig config : splitUrlConfigs) {
            try {
                ScmSession scmSession = ScmFactory.Session.priorityAccess(sessionType, config,
                        sessionPoolConf.getSessionConfig(), this);
                if (sessionType == SessionType.AUTH_SESSION) {
                    lastAccessTimeMap.put(scmSession.getSessionId(), System.currentTimeMillis());
                }
                return scmSession;
            }
            catch (ScmException e) {
                lastException = e;
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new ScmInvalidArgumentException(
                "no available url, urls=" + lastUsedUrlConfig.getUrl());
    }

    private void beforeGetSession() throws ScmException {
        if (!nodeGroupInitialed) {
            synchronized (this) {
                if (!nodeGroupInitialed) {
                    refreshUrlNodeGroup();
                    nodeGroupInitialed = true;
                }
            }
        }
    }

    private void refreshUrlNodeGroup() throws ScmException {
        if (sessionPoolConf.getNodeGroup() == null) {
            return;
        }
        ScmSession scmSession = null;
        try {
            scmSession = createTempNotAuthSession();
            List<ScmServiceInstance> gatewayList = ScmSystem.ServiceCenter
                    .getServiceInstanceList(scmSession, "gateway");
            for (UrlInfo urlInfo : this.gatewayUrlInfo.values()) {
                String[] split = (getPureUrl(urlInfo.getUrl())).split(":");
                String host = split[0];
                int port = Integer.parseInt(split[1]);
                for (ScmServiceInstance gateway : gatewayList) {
                    if ((host.equals(gateway.getHostName())
                            || gateway.getAllValidIp().contains(host))
                            && port == gateway.getPort()) {
                        BSONObject metadata = gateway.getMetadata();
                        if (metadata != null) {
                            urlInfo.setNodeGroup((String) metadata.get(NODE_GROUP));
                        }
                    }
                }
            }
            nodeGroupInitialed = true;
        }
        finally {
            if (scmSession != null) {
                scmSession.close();
            }
        }
    }

    private List<ScmUrlConfig> splitAndSortUrlConfig(ScmUrlConfig urlConfig)
            throws ScmInvalidArgumentException {
        List<ScmUrlConfig> urlConfigs = new ArrayList<ScmUrlConfig>();
        String nodeGroup = sessionPoolConf.getNodeGroup();
        if (nodeGroup == null) {
            urlConfigs.add(urlConfig);
            return urlConfigs;
        }
        String zone = sessionPoolConf.getSessionConfig().getZone();
        ScmUrlConfig.Builder sameZoneAndSameGroup = ScmUrlConfig.custom();
        ScmUrlConfig.Builder sameZoneAndDiffGroup = ScmUrlConfig.custom();
        ScmUrlConfig.Builder diffZoneAndSameGroup = ScmUrlConfig.custom();
        ScmUrlConfig.Builder diffZoneAndDiffGroup = ScmUrlConfig.custom();

        for (String url : urlConfig.getUrl()) {
            UrlInfo urlInfo = gatewayUrlInfo.get(getPureUrl(url));
            if (urlInfo.getZone().equals(zone) && nodeGroup.equals(urlInfo.getNodeGroup())) {
                sameZoneAndSameGroup.addUrl(urlInfo.getRegion(), urlInfo.getZone(),
                        Collections.singletonList(urlInfo.getUrl()));
            }
            else if (urlInfo.getZone().equals(zone) && !nodeGroup.equals(urlInfo.getNodeGroup())) {
                sameZoneAndDiffGroup.addUrl(urlInfo.getRegion(), urlInfo.getZone(),
                        Collections.singletonList(urlInfo.getUrl()));
            }
            else if (!urlInfo.getZone().equals(zone) && nodeGroup.equals(urlInfo.getNodeGroup())) {
                diffZoneAndSameGroup.addUrl(urlInfo.getRegion(), urlInfo.getZone(),
                        Collections.singletonList(urlInfo.getUrl()));
            }
            else if (!urlInfo.getZone().equals(zone) && !nodeGroup.equals(urlInfo.getNodeGroup())) {
                diffZoneAndDiffGroup.addUrl(urlInfo.getRegion(), urlInfo.getZone(),
                        Collections.singletonList(urlInfo.getUrl()));
            }
        }

        if (sessionPoolConf.getGroupAccessMode() == ScmType.NodeGroupAccessMode.ACROSS) {
            ScmUrlConfig tempConfig = sameZoneAndSameGroup.build();
            if (!tempConfig.getUrl().isEmpty()) {
                urlConfigs.add(tempConfig);
            }
            tempConfig = sameZoneAndDiffGroup.build();
            if (!tempConfig.getUrl().isEmpty()) {
                urlConfigs.add(tempConfig);
            }
            tempConfig = diffZoneAndSameGroup.build();
            if (!tempConfig.getUrl().isEmpty()) {
                urlConfigs.add(tempConfig);
            }
            tempConfig = diffZoneAndDiffGroup.build();
            if (!tempConfig.getUrl().isEmpty()) {
                urlConfigs.add(tempConfig);
            }
        }
        else if (sessionPoolConf.getGroupAccessMode() == ScmType.NodeGroupAccessMode.ALONG) {
            ScmUrlConfig tempConfig = sameZoneAndSameGroup.build();
            if (!tempConfig.getUrl().isEmpty()) {
                urlConfigs.add(tempConfig);
            }
        }
        else {
            throw new ScmInvalidArgumentException(
                    "unrecognized groupAccessMode:" + sessionPoolConf.getGroupAccessMode());
        }
        if (urlConfigs.isEmpty()) {
            throw new ScmInvalidArgumentException("no available url for nodeGroup:" + nodeGroup
                    + ", groupAccessMode=" + sessionPoolConf.getGroupAccessMode() + ", urls="
                    + urlConfig.getUrl());
        }
        return urlConfigs;
    }

    ScmSession createTempNotAuthSession() throws ScmException {
        return ScmFactory.Session.priorityAccess(SessionType.AUTH_SESSION,
                sessionPoolConf.getSessionConfig().getUrlConfig(),
                sessionPoolConf.getSessionConfig(), null);
    }

    private boolean isAvailable(ScmSession scmSession) {
        if (isExpired(scmSession)) {
            logger.debug("session is expired: " + scmSession);
            return false;
        }
        if (!isHealth(scmSession)) {
            logger.debug("session is unhealthy:" + scmSession);
            return false;
        }
        return true;
    }

    private boolean isExpired(ScmSession scmSession) {
        // only sessions of type AUTH_SESSION can expire.
        if (scmSession.getType() == SessionType.AUTH_SESSION) {
            Long lastAccessTime = lastAccessTimeMap.get(scmSession.getSessionId());
            if (lastAccessTime == null) {
                // should never come here
                return true;
            }
            long now = System.currentTimeMillis();
            return (now - lastAccessTime) / 1000 >= sessionPoolConf.getKeepAliveTime();
        }
        return false;
    }

    private boolean isHealth(ScmSession scmSession) {
        String gatewayUrl = getPureUrl(scmSession.getUrl());
        return isGatewayUrlHealth(gatewayUrl);
    }

    private boolean isGatewayUrlHealth(String gatewayUrl) {
        Boolean health = gatewayHealthStatus.get(gatewayUrl);
        return health == null || health;
    }

    private void addToUrlInfo(List<UrlInfo> urlInfo) {
        for (UrlInfo info : urlInfo) {
            gatewayUrlInfo.put(getPureUrl(info.getUrl()), info);
        }
    }

    private String getPureUrl(String url) {
        int idx = url.indexOf("/");
        if (-1 != idx) {
            return url.substring(0, idx);
        }
        return url;
    }

    private ScmSession pollFirst(SessionType sessionType)
            throws ScmInvalidArgumentException {
        ScmSession scmSession = null;
        if (sessionType == SessionType.AUTH_SESSION) {
            scmSession = authSessionQueue.pollFirst();
        }
        else if (sessionType == SessionType.NOT_AUTH_SESSION) {
            scmSession = notAuthSessionQueue.pollFirst();
        }
        else {
            throw new ScmInvalidArgumentException("unsupported session type:" + sessionType);
        }
        return scmSession;
    }

    private boolean offerFirst(ScmSession scmSession) throws ScmInvalidArgumentException {
        if (scmSession.getType() == SessionType.AUTH_SESSION) {
            return authSessionQueue.offerFirst(scmSession);
        }
        else if (scmSession.getType() == SessionType.NOT_AUTH_SESSION) {
            return notAuthSessionQueue.offerFirst(scmSession);
        }
        else {
            throw new ScmInvalidArgumentException(
                    "unsupported session type, session:" + scmSession);
        }
    }

    private int sessionCount() {
        return authSessionQueue.size() + notAuthSessionQueue.size();
    }

    void closeAbnormalSessions() {
        ScmSession session = null;
        while ((session = abnormalSessionQueue.pollFirst()) != null) {
            closeSessionSilence(session);
        }
    }

    private void closeAllSession() {
        ScmSession session = null;
        while ((session = authSessionQueue.pollFirst()) != null) {
            closeSessionSilence(session);
        }
        while ((session = notAuthSessionQueue.pollFirst()) != null) {
            closeSessionSilence(session);
        }
        closeAbnormalSessions();
    }

    @Override
    public void close() {
        Lock lock = closedStateLock.writeLock();
        lock.lock();
        try {
            if (closed) {
                return;
            }
            closeAllSession();
            releaseResource();
            closed = true;
        }
        catch (Exception e) {
            logger.warn("close sessionMgr failed", e);
        }
        finally {
            lock.unlock();
        }

    }

    private void releaseResource() {
        try {
            if (syncGatewayAddrTaskTimer != null) {
                syncGatewayAddrTaskTimer.cancel();
            }
            if (checkGatewayAddrTaskTimer != null) {
                checkGatewayAddrTaskTimer.cancel();
            }
            if (clearAbnormalSessionsTaskTimer != null) {
                clearAbnormalSessionsTaskTimer.cancel();
            }
            if (idleConnectionMonitorTimer != null) {
                idleConnectionMonitorTimer.cancel();
            }
            if (refreshNodeGroupTimer != null) {
                refreshNodeGroupTimer.cancel();
            }
            if (httpClient != null && outerSessions.isEmpty()) {
                httpClient.close();
            }
            authSessionQueue = null;
            notAuthSessionQueue = null;
            abnormalSessionQueue = null;
            lastAccessTimeMap.clear();
            gatewayHealthStatus.clear();
            gatewayUrlInfo.clear();
        }
        catch (Exception e) {
            logger.warn("failed to release resource", e);
        }
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    private CloseableHttpClient createHttpClient() {
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager();
        connMgr.setMaxTotal(sessionPoolConf.getMaxConnections());
        connMgr.setDefaultMaxPerRoute(sessionPoolConf.getMaxConnections());
        idleConnectionMonitorTask = new IdleConnectionMonitorTask(connMgr);
        ScmRequestConfig requestConfig = sessionPoolConf.getSessionConfig().getRequestConfig();
        if (requestConfig == null) {
            requestConfig = ScmRequestConfig.custom().build();
        }
        RequestConfig reqConf = RequestConfig.custom()
                .setConnectionRequestTimeout(sessionPoolConf.getConnectionRequestTimeout())
                .setSocketTimeout(requestConfig.getSocketTimeout())
                .setConnectTimeout(requestConfig.getConnectTimeout()).build();

        HttpResponseInterceptor responseInterceptor = new HttpResponseInterceptor() {
            @Override
            public void process(HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                Header[] headers = response.getHeaders(AUTHORIZATION);
                if (headers != null && headers.length > 0) {
                    String sessionId = headers[0].getValue();
                    if (sessionId != null && !sessionId.isEmpty()) {
                        lastAccessTimeMap.put(sessionId, System.currentTimeMillis());
                    }
                }
            }
        };

        HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(connMgr)
                .setDefaultRequestConfig(reqConf).addInterceptorLast(responseInterceptor)
                .setRetryHandler(new HttpRequestRetryHandler() {
                    @Override
                    public boolean retryRequest(IOException exception, int executionCount,
                            HttpContext context) {
                        if (exception instanceof ConnectionPoolTimeoutException) {
                            throw new RuntimeException(
                                    "the maximum number of connections for sessionMgr has been exceeded, max connections:"
                                            + sessionPoolConf.getMaxConnections(),
                                    exception);
                        }
                        if (exception instanceof ConnectException) {
                            HttpHost httpHost = (HttpHost) context
                                    .getAttribute(HTTP_CLIENT_HOST_ATTRIBUTE);
                            if (httpHost != null) {
                                String url = httpHost.getHostName() + ":" + httpHost.getPort();
                                recordGatewayHealthStatus(url, false);
                            }
                        }
                        if (exception instanceof NoHttpResponseException && executionCount <= 1) {
                            return true;
                        }
                        return false;
                    }
                });
        return httpClientBuilder.build();
    }

    void recordGatewayHealthStatus(String gatewayUrl, boolean status) {
        boolean hasChange = false;
        Boolean beforeStatus = gatewayHealthStatus.get(gatewayUrl);
        if (beforeStatus == null || beforeStatus != status) {
            hasChange = true;
        }
        gatewayHealthStatus.put(gatewayUrl, status);

        if (hasChange) {
            try {
                removeUnhealthyUrlFromUrlConfig();
            }
            catch (ScmInvalidArgumentException e) {
                logger.warn("failed to remove unhealthy url:{}", gatewayUrl, e);
            }
        }
        if (!status) {
            logger.debug("mark gateway unhealthy: " + gatewayUrl);
        }
        else {
            logger.debug("mark gateway healthy: " + gatewayUrl);
            try {
                addNewUrlToUrlConfig(gatewayUrl);
            }
            catch (ScmInvalidArgumentException e) {
                logger.warn("failed to add healthy url:{}", gatewayUrl, e);
            }
        }
    }

    private synchronized void updateScmUrlConfig(List<UrlInfo> newUrls)
            throws ScmInvalidArgumentException {
        ScmUrlConfig.Builder builder = ScmUrlConfig.custom();
        List<UrlInfo> urlInfo = this.basicUrl.getUrlInfo();
        for (UrlInfo info : urlInfo) {
            if (isGatewayUrlHealth(getPureUrl(info.getUrl()))) {
                builder.addUrl(info.getRegion(), info.getZone(),
                        Collections.singletonList(info.getUrl()));
            }
        }
        for (UrlInfo newUrl : newUrls) {
            builder.addUrl(newUrl.getRegion(), newUrl.getZone(),
                    Collections.singletonList(newUrl.getUrl()));
        }
        this.sessionPoolConf.getSessionConfig().setUrlConfig(builder.build());
    }

    private synchronized void addNewUrlToUrlConfig(String pureUrl)
            throws ScmInvalidArgumentException {
        UrlInfo toAddUrl = gatewayUrlInfo.get(pureUrl);
        if (toAddUrl == null) {
            return;
        }
        ScmUrlConfig oldUrlConfig = this.sessionPoolConf.getSessionConfig().getUrlConfig();
        List<UrlInfo> urlInfo = oldUrlConfig.getUrlInfo();
        if (!urlInfo.contains(toAddUrl)) {
            ScmUrlConfig build = ScmUrlConfig.custom(oldUrlConfig).addUrl(toAddUrl.getRegion(),
                    toAddUrl.getZone(), Collections.singletonList(toAddUrl.getUrl())).build();
            this.sessionPoolConf.getSessionConfig().setUrlConfig(build);
        }
    }

    private synchronized void removeUnhealthyUrlFromUrlConfig() throws ScmInvalidArgumentException {
        List<UrlInfo> urlInfo = this.sessionPoolConf.getSessionConfig().getUrlConfig().getUrlInfo();
        Iterator<UrlInfo> iterator = urlInfo.iterator();
        boolean removed = false;
        while (iterator.hasNext()) {
            if (!isGatewayUrlHealth(getPureUrl(iterator.next().getUrl()))) {
                iterator.remove();
                removed = true;
            }
        }
        if (!removed) {
            return;
        }
        if (urlInfo.size() <= 0) {
            // recovery
            this.sessionPoolConf.getSessionConfig()
                    .setUrlConfig(ScmUrlConfig.custom(this.basicUrl).build());
        }
        else {
            ScmUrlConfig.Builder builder = ScmUrlConfig.custom();
            for (UrlInfo info : urlInfo) {
                builder.addUrl(info.getRegion(), info.getZone(),
                        Collections.singletonList(info.getUrl()));
            }
            this.sessionPoolConf.getSessionConfig().setUrlConfig(builder.build());
        }
    }

    static class ScmSessionQueue {

        private final Deque<ScmSession> queue;

        private final Set<ScmSession> offeredSessions = new HashSet<ScmSession>();

        public ScmSessionQueue(int capacity) {
            queue = new ArrayDeque<ScmSession>(capacity);
        }

        public synchronized ScmSession pollFirst() {
            ScmSession session = queue.pollFirst();
            if (session != null) {
                offeredSessions.remove(session);
            }
            return session;
        }

        public synchronized boolean offerFirst(ScmSession session) {
            if (offeredSessions.contains(session)) {
                return true;
            }
            if (queue.offerFirst(session)) {
                offeredSessions.add(session);
                return true;
            }
            return false;
        }

        public synchronized int size() {
            return queue.size();
        }

    }

}

class IdleConnectionMonitorTask extends ScmTimerTask {
    private final HttpClientConnectionManager connMgr;

    IdleConnectionMonitorTask(HttpClientConnectionManager connMgr) {
        this.connMgr = connMgr;
    }

    @Override
    public void run() {
        // Close expired connections
        connMgr.closeExpiredConnections();
        // Optionally, close connections
        // that have been idle longer than 30 sec
        connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
    }
}

class CheckGatewayUrlsTask extends ScmTimerTask {

    private final Logger logger = LoggerFactory.getLogger(CheckGatewayUrlsTask.class);

    private final RestDispatcher restDispatcher;

    private final ScmPoolingSessionMgrImpl sessionMgr;

    private final UrlsProvider urlsProvider;

    public CheckGatewayUrlsTask(CloseableHttpClient httpClient, ScmPoolingSessionMgrImpl sessionMgr,
            UrlsProvider urlsProvider) {
        this.restDispatcher = new RestDispatcher("", null, httpClient);
        this.sessionMgr = sessionMgr;
        this.urlsProvider = urlsProvider;
    }

    @Override
    public void run() {
        Set<String> urls = urlsProvider.getUrls();
        logger.debug("begin check gateway urls: " + urls);
        if (urls == null || urls.size() <= 0) {
            return;
        }
        for (String url : urls) {
            boolean isHealth = ScmHelper.checkGatewayHealth(url, restDispatcher);
            sessionMgr.recordGatewayHealthStatus(url, isHealth);
            if (!isHealth) {
                logger.info("gateway node is unhealthy: " + url);
            }
        }
    }

    public interface UrlsProvider {
        Set<String> getUrls();
    }
}

class ClearAbnormalSessionsTask extends ScmTimerTask {

    private final Logger logger = LoggerFactory.getLogger(ClearAbnormalSessionsTask.class);

    private final ScmPoolingSessionMgrImpl sessionMgr;

    public ClearAbnormalSessionsTask(ScmPoolingSessionMgrImpl sessionMgr) {
        this.sessionMgr = sessionMgr;
    }

    @Override
    public void run() {
        logger.debug("begin clear abnormal session");
        sessionMgr.closeAbnormalSessions();
    }
}

interface SyncGatewayAddrCallback {
    void onNewUrlSync(List<UrlInfo> newUrls);
}

class SyncGatewayAddrTask extends ScmTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(SyncGatewayAddrTask.class);
    private ScmPoolingSessionMgrImpl sessionMgr;
    private ScmUrlConfig basicUrlConfig;
    private Map<String, String> basicIpUrlsMap;
    private SyncGatewayAddrCallback callback;
    private Map<String, Integer> ipSimilarityMap = new HashMap<String, Integer>();

    public SyncGatewayAddrTask(ScmPoolingSessionMgrImpl sessionMgr, ScmUrlConfig basicUrlConfig,
            SyncGatewayAddrCallback callback)
            throws ScmInvalidArgumentException {
        this.sessionMgr = sessionMgr;
        this.basicUrlConfig = basicUrlConfig;
        this.basicIpUrlsMap = getIpUrls(basicUrlConfig.getUrl());
        this.callback = callback;
    }

    @Override
    public void run() {
        ScmSession ss = null;
        try {
            ss = sessionMgr.createTempNotAuthSession();
            List<ScmServiceInstance> gatewayInstances = ScmSystem.ServiceCenter
                    .getServiceInstanceList(ss, "gateway");
            if (gatewayInstances.size() <= 0) {
                logger.warn("latest gateway addr is empty");
                return;
            }

            String targetSite = basicUrlConfig.getTargetSite();
            String urlTail = targetSite == null ? "" : "/" + targetSite;
            List<UrlInfo> remoteNewUrls = new ArrayList<UrlInfo>();
            for (ScmServiceInstance instance : gatewayInstances) {
                if (!"UP".equals(instance.getStatus())) {
                    continue;
                }
                String ip = getAvailableInstanceIp(instance, ss);
                if (ip == null) {
                    continue;
                }
                String instanceUrl = ip + ":" + instance.getPort() + urlTail;
                if (!basicIpUrlsMap.containsKey(instanceUrl)) {
                    UrlInfo urlInfo = new UrlInfo(instanceUrl, instance.getRegion(),
                            instance.getZone());
                    BSONObject metadata = instance.getMetadata();
                    if (metadata != null) {
                        urlInfo.setNodeGroup(
                                (String) metadata.get(ScmPoolingSessionMgrImpl.NODE_GROUP));
                    }
                    remoteNewUrls.add(urlInfo);
                }
            }
            if (remoteNewUrls.size() > 0) {
                callback.onNewUrlSync(remoteNewUrls);
                logger.debug("sync new gateway addr:" + remoteNewUrls);
            }

        }
        catch (Exception e) {
            logger.warn("failed to sync gateway addr list", e);
        }
        finally {
            if (ss != null) {
                ss.close();
            }
        }
    }

    private String getAvailableInstanceIp(ScmServiceInstance instance, ScmSession ss) {
        Set<IpWithSimilarity> ipSet = new TreeSet<IpWithSimilarity>();
        for (String ip : instance.getAllValidIp()) {
            ipSet.add(new IpWithSimilarity(ip, getIpSimilarity(ip)));
        }
        for (IpWithSimilarity ipWithSimilarity : ipSet) {
            String ip = ipWithSimilarity.getIp();
            if (ScmHelper.checkGatewayHealth(ip + ":" + instance.getPort(), ss.getDispatcher())) {
                logger.debug("chosen gateway url: {}:{}", ip, instance.getPort());
                return ip;
            }
            else {
                logger.debug("gateway url is unhealthy: ip={},port={}", ip, instance.getPort());
            }
        }
        logger.warn("no available ip for gateway instance:{}, ipList={}", instance, ipSet);
        return null;
    }

    private int getIpSimilarity(String ip) {
        Integer similarity = ipSimilarityMap.get(ip);
        if (similarity != null) {
            return similarity;
        }
        similarity = getCommonPrefixLength(ip, getFirstBasicIp());
        ipSimilarityMap.put(ip, similarity);
        return similarity;
    }

    private static int getCommonPrefixLength(String str1, String str2) {
        int minLen = Math.min(str1.length(), str2.length());
        int count = 0;
        for (int i = 0; i < minLen; i++) {
            if (str1.charAt(i) == str2.charAt(i)) {
                count++;
            }
            else {
                break;
            }
        }
        return count;
    }

    private String getFirstBasicIp() {
        String basicIpUrl = this.basicIpUrlsMap.keySet().iterator().next();
        return basicIpUrl.substring(0, basicIpUrl.indexOf(":"));
    }

    private Map<String, String> getIpUrls(List<String> urls) throws ScmInvalidArgumentException {
        Map<String, String> ipUrlsMap = new LinkedHashMap<String, String>();
        for (String basicUrl : urls) {
            int index = basicUrl.indexOf(":");
            if (index <= -1) {
                throw new ScmInvalidArgumentException("invalid url:" + basicUrl);
            }
            String host = basicUrl.substring(0, index);
            String ip;
            try {
                ip = InetAddress.getByName(host).getHostAddress();
            }
            catch (UnknownHostException e) {
                throw new ScmInvalidArgumentException("invalid url:" + basicUrl, e);
            }
            ipUrlsMap.put(ip + basicUrl.substring(index), basicUrl);
        }
        return ipUrlsMap;
    }

    static class IpWithSimilarity implements Comparable<IpWithSimilarity> {

        private String ip;
        private int similarity;

        public IpWithSimilarity(String ip, int similarity) {
            this.ip = ip;
            this.similarity = similarity;
        }

        @Override
        public int compareTo(IpWithSimilarity other) {
            if (ip.equals(other.getIp())) {
                return 0;
            }
            int res = other.getSimilarity() - this.similarity;
            if (res == 0) {
                return this.compareTo(other);
            }
            else {
                return res;
            }
        }

        public String getIp() {
            return ip;
        }

        public int getSimilarity() {
            return similarity;
        }

    }

}
