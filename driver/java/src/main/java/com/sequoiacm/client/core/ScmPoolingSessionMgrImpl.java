package com.sequoiacm.client.core;

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
    private static final String AUTHORIZATION = "x-auth-token";
    private static final String HTTP_CLIENT_HOST_ATTRIBUTE = "http.target_host";

    private ScmSessionQueue authSessionQueue;
    private ScmSessionQueue notAuthSessionQueue;
    private ScmSessionQueue abnormalSessionQueue;

    private ScmTimer syncGatewayAddrTaskTimer;
    private ScmTimer checkGatewayAddrTaskTimer;
    private ScmTimer clearAbnormalSessionsTaskTimer;
    private ScmTimer idleConnectionMonitorTimer;

    // Dummy value to associate with an Object in the outerSessions Map
    private static final Object PRESENT = new Object();
    private final Map<ScmSession, Object> outerSessions = new ConcurrentHashMap<ScmSession, Object>();


    private IdleConnectionMonitorTask idleConnectionMonitorTask;

    private Map<String, Long> lastAccessTimeMap = new ConcurrentHashMap<String, Long>();

    private Map<String, Boolean> gatewayHealthStatus = new ConcurrentHashMap<String, Boolean>();

    private final CloseableHttpClient httpClient;

    private final ScmSessionPoolConf sessionPoolConf;

    private final ReadWriteLock closedStateLock = new ReentrantReadWriteLock();

    private boolean closed = false;

    ScmPoolingSessionMgrImpl(ScmSessionPoolConf conf) throws ScmException {
        try {
            this.sessionPoolConf = conf;
            this.authSessionQueue = new ScmSessionQueue(conf.getMaxCacheSize());
            this.notAuthSessionQueue = new ScmSessionQueue(conf.getMaxCacheSize());
            this.abnormalSessionQueue = new ScmSessionQueue(ABNORMAL_SESSION_QUEUE_SIZE);
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
        final SyncGatewayAddrTask syncGatewayAddrTask = new SyncGatewayAddrTask(this,
                sessionPoolConf.getSessionConfig());
        if (interval > 0) {
            syncGatewayAddrTaskTimer = ScmTimerFactory.createScmTimer();
            syncGatewayAddrTaskTimer.schedule(syncGatewayAddrTask, interval, interval);
        }

        // start check gateway urls task
        interval = sessionPoolConf.getCheckGatewayUrlsInterval();
        CheckGatewayUrlsTask checkGatewayUrlsTask = new CheckGatewayUrlsTask(httpClient,
                gatewayHealthStatus, new CheckGatewayUrlsTask.UrlsProvider() {
                    @Override
                    public List<String> getUrls() {
                        List<String> currentUrls = syncGatewayAddrTask.getCurrentUrls();
                        List<String> pureUrls = new ArrayList<String>(currentUrls.size());
                        // host:ip/siteName => host:ip
                        for (String url : currentUrls) {
                            pureUrls.add(getPureUrl(url));
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

        // start httpClient idleConnectionMonitor task
        idleConnectionMonitorTimer = ScmTimerFactory.createScmTimer();
        idleConnectionMonitorTimer.schedule(idleConnectionMonitorTask,
                HTTP_CLIENT_IDLE_CONNECTION_MONITOR_INTERVAL,
                HTTP_CLIENT_IDLE_CONNECTION_MONITOR_INTERVAL);
    }

    @Override
    public ScmSession getSession(SessionType sessionType) throws ScmException {
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
        ScmSession scmSession = ScmFactory.Session.priorityAccess(sessionType,
                sessionPoolConf.getSessionConfig().getUrlConfig(),
                sessionPoolConf.getSessionConfig(), this);
        if (sessionType == SessionType.AUTH_SESSION) {
            lastAccessTimeMap.put(scmSession.getSessionId(), System.currentTimeMillis());
        }
        return scmSession;
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
        Boolean health = gatewayHealthStatus.get(gatewayUrl);
        return health == null || health;
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
            if (httpClient != null && outerSessions.isEmpty()) {
                httpClient.close();
            }
            authSessionQueue = null;
            notAuthSessionQueue = null;
            abnormalSessionQueue = null;
            lastAccessTimeMap.clear();
            gatewayHealthStatus.clear();
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
                                markGatewayUnhealthy(url);
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

    private void markGatewayUnhealthy(String url) {
        logger.debug("mark gateway unhealthy: " + url);
        gatewayHealthStatus.put(url, false);
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

    private final Map<String, Boolean> statusMap;

    private final UrlsProvider urlsProvider;

    public CheckGatewayUrlsTask(CloseableHttpClient httpClient, Map<String, Boolean> statusMap,
            UrlsProvider urlsProvider) {
        this.restDispatcher = new RestDispatcher("", null, httpClient);
        this.statusMap = statusMap;
        this.urlsProvider = urlsProvider;
    }

    @Override
    public void run() {
        List<String> urls = urlsProvider.getUrls();
        logger.debug("begin check gateway urls: " + urls);
        if (urls == null || urls.size() <= 0) {
            return;
        }
        for (String url : urls) {
            boolean isHealth = ScmHelper.checkGatewayHealth(url, restDispatcher);
            statusMap.put(url, isHealth);
            if (!isHealth) {
                logger.info("gateway node is unhealthy: " + url);
            }
        }
    }

    public interface UrlsProvider {
        List<String> getUrls();
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

class SyncGatewayAddrTask extends ScmTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(SyncGatewayAddrTask.class);
    private ScmSessionMgr sessionMgr;
    private ScmUrlConfig basicUrlConfig;
    private Map<String, String> basicIpUrlsMap;
    private ScmConfigOption configOption;

    private ScmUrlConfig remoteUrlConfig;
    private ScmUrlConfig currentUrlConfig;
    private List<ScmGatewayUrl> remoteNewUrls;

    public SyncGatewayAddrTask(ScmSessionMgr sessionMgr, ScmConfigOption configOption)
            throws ScmInvalidArgumentException {
        this.sessionMgr = sessionMgr;
        this.configOption = configOption;
        this.basicUrlConfig = configOption.getUrlConfig();
        this.currentUrlConfig = configOption.getUrlConfig();
        this.basicIpUrlsMap = getIpUrls(basicUrlConfig.getUrl());
    }

    @Override
    public void run() {
        ScmSession ss = null;
        try {
            ss = sessionMgr.getSession(SessionType.NOT_AUTH_SESSION);
            List<ScmServiceInstance> gatewayInstances = ScmSystem.ServiceCenter
                    .getServiceInstanceList(ss, "gateway");
            if (gatewayInstances.size() <= 0) {
                logger.warn("latest gateway addr is empty");
                resetUrlConfig();
                return;
            }

            String targetSite = configOption.getUrlConfig().getTargetSite();
            String urlTail = targetSite == null ? "" : "/" + targetSite;

            ScmUrlConfig.Builder remoteUrlsBuilder = ScmUrlConfig.custom();
            remoteNewUrls = new ArrayList<ScmGatewayUrl>();

            for (ScmServiceInstance instance : gatewayInstances) {
                if (!"UP".equals(instance.getStatus())) {
                    continue;
                }
                String instanceUrl = instance.getIp() + ":" + instance.getPort() + urlTail;
                if (!basicIpUrlsMap.containsKey(instanceUrl)) {
                    remoteNewUrls.add(new ScmGatewayUrl(instanceUrl, instance.getRegion(),
                            instance.getZone()));
                    remoteUrlsBuilder.addUrl(instance.getRegion(), instance.getZone(),
                            Collections.singletonList(instanceUrl));
                }
                else {
                    remoteUrlsBuilder.addUrl(instance.getRegion(), instance.getZone(),
                            Collections.singletonList(basicIpUrlsMap.get(instanceUrl)));
                }
            }
            remoteUrlConfig = remoteUrlsBuilder.build();
            if (remoteUrlConfig.getUrl().size() > 0) {
                configOption.setUrlConfig(remoteUrlConfig);
                currentUrlConfig = remoteUrlConfig;
            }
            logger.debug("sync gateway addr:" + remoteUrlConfig.getUrl());
        }
        catch (Exception e) {
            logger.warn("failed to sync gateway addr list", e);
            resetUrlConfig();
        }
        finally {
            if (ss != null) {
                ss.close();
            }
        }
    }

    private void resetUrlConfig() {
        try {
            ScmUrlConfig urlConfig = ScmUrlConfig.custom(basicUrlConfig).build();
            if (remoteNewUrls != null && remoteNewUrls.size() > 0) {
                for (ScmGatewayUrl remoteNewUrl : remoteNewUrls) {
                    urlConfig.addUrl(remoteNewUrl.getRegion(), remoteNewUrl.getZone(),
                            Collections.singletonList(remoteNewUrl.getZone()));
                }
            }
            configOption.setUrlConfig(urlConfig);
            this.currentUrlConfig = urlConfig;
            logger.debug("reset gateway addr list: " + urlConfig.getUrl());
        }
        catch (ScmInvalidArgumentException e) {
            logger.warn("failed to reset gateway addr list", e);
        }

    }

    private Map<String, String> getIpUrls(List<String> urls) throws ScmInvalidArgumentException {
        Map<String, String> ipUrlsMap = new HashMap<String, String>();
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

    public List<String> getCurrentUrls() {
        return currentUrlConfig.getUrl();
    }

    static class ScmGatewayUrl {
        private String url;
        private String region;
        private String zone;

        public ScmGatewayUrl(String url, String region, String zone) {
            this.url = url;
            this.region = region;
            this.zone = zone;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }
    }

}
