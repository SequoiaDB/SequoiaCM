package com.sequoiacm.s3.remote;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.sequoiacm.infrastructrue.security.core.AccesskeyInfo;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import com.sequoiacm.s3.authoriztion.ScmSession;
import com.sequoiacm.s3.config.ScmConnectionConfig;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import com.sequoiacm.s3.utils.CommonUtil;

@Component
public class ScmClientFactory {
    private static final ObjectMapper SCM_OBJMAPPER = new ObjectMapper();
    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ScmDirInfo.class, new ScmDirInfoDeserializer());
        module.addDeserializer(ScmFileInfo.class, new ScmFileInfoDeserializer());
        module.addDeserializer(ScmWsInfo.class, new ScmWsInfoDeserializer());
        module.addDeserializer(AccesskeyInfo.class, new AccesskeyInfoDeserializer());
        SCM_OBJMAPPER.registerModule(module);
    }
    private Map<String, ContenServerService> contentServerServices = new ConcurrentHashMap<>();

    @Autowired
    private ScmFeignClient feign;
    private CloseableHttpClient httpClient;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    private volatile Map<Integer, SiteInfo> siteInfos = new ConcurrentHashMap<>();
    private volatile SiteInfo rootSite;
    private Map<String, WorkspacePreferedSite> workspacePreferSites = new ConcurrentHashMap<>();

    @Value("${eureka.instance.metadata-map.zone:#{null}}")
    private String zone;
    @Value("${eureka.client.region:#{null}}")
    private String region;

    private PoolingHttpClientConnectionManager connectionManager;

    private ScmTimer timer;

    private volatile AuthServerService authService;

    private ScmConnectionConfig config;

    @Autowired
    public ScmClientFactory(ScmConnectionConfig config) {
        this.config = config;
        timer = ScmTimerFactory.createScmTimer();
        try {
            initHttpClient(config);
            timer.schedule(new SiteInfoRefresher(this), 30000, config.getSiteInfoRefreshInterval());
        }
        catch (Exception e) {
            destory();
            throw e;
        }
    }

    private void initHttpClient(ScmConnectionConfig config) {
        connectionManager = new PoolingHttpClientConnectionManager(config.getConnectionTimeToLive(),
                TimeUnit.SECONDS);
        connectionManager.setMaxTotal(config.getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(config.getMaxPerRouteConnections());

        timer.schedule(new HttpConnectionCleaner(connectionManager), 30000,
                config.getConnectionCleanerRepeatInterval());
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(config.getConnectTimeout())
                .setSocketTimeout(config.getSocketTimeout())
                .setConnectionRequestTimeout(config.getConnectionRequestTimeout())
                .setRedirectsEnabled(false).build();
        httpClient = HttpClientBuilder.create().disableContentCompression()
                .disableCookieManagement().useSystemProperties()
                .setDefaultRequestConfig(defaultRequestConfig)
                .setConnectionManager(connectionManager).build();
    }

    public AuthServerClient getAuthServerClient(ScmSession session) {
        if (authService == null) {
            authService = feign.builder().objectMapper(SCM_OBJMAPPER)
                    .serviceTarget(AuthServerService.class, "auth-server");
        }
        return new AuthServerClient(session, authService);
    }

    public ScmContentServerClient getContentServerClient(ScmSession session, String ws)
            throws S3ServerException {
        String site = getSite(session, ws);
        ContenServerService contentserverService = getScmFeignService(site);
        return new ScmContentServerClient(session, ws, site, feign, httpClient, loadBalancerClient,
                contentserverService);
    }

    private ContenServerService getScmFeignService(String site) {
        ContenServerService contentserverService = contentServerServices.get(site);
        if (contentserverService == null) {
            contentserverService = feign.builder().objectMapper(SCM_OBJMAPPER)
                    .serviceTarget(ContenServerService.class, site.toLowerCase());
            contentServerServices.put(site, contentserverService);
        }
        return contentserverService;
    }

    private String getSite(ScmSession session, String ws) throws S3ServerException {
        if (ws == null) {
            return getRootSite();
        }
        return getPreferSite(session, ws);
    }

    private String getPreferSite(ScmSession session, String ws) throws S3ServerException {
        WorkspacePreferedSite preferedSite = workspacePreferSites.get(ws);
        if (preferedSite != null && !preferedSite.isExpired(config.getWorkspaceCacheTTL())) {
            return preferedSite.getSiteName();
        }

        ScmContentServerClient client = new ScmContentServerClient(session, ws, getRootSite(),
                feign, httpClient, loadBalancerClient, getScmFeignService(getRootSite()));
        ScmWsInfo wsInfo;
        try {
            wsInfo = client.getWorkspace(ws);
        }
        catch (ScmFeignException e) {
            throw new S3ServerException(S3Error.SCM_GET_WS_FAILED, "get worskapce failed:" + ws, e);
        }
        if (wsInfo == null) {
            throw new S3ServerException(S3Error.REGION_NO_SUCH_REGION, "worksapce not found:" + ws);
        }
        List<Integer> siteIds = wsInfo.getSites();
        String preferdSiteName = null;
        String myZone = region + "-" + zone;
        for (Integer siteId : siteIds) {
            SiteInfo siteInfo = siteInfos.get(siteId);
            if (siteInfo == null) {
                refreshSiteCache();
                siteInfo = siteInfos.get(siteId);
                if (siteInfo == null) {
                    continue;
                }
            }

            if (siteInfo.getRegion().contains(myZone)) {
                preferdSiteName = siteInfo.getName();
                break;
            }
            if (preferdSiteName == null) {
                preferdSiteName = siteInfo.getName();
            }
        }
        if (preferdSiteName != null) {
            workspacePreferSites.put(ws, new WorkspacePreferedSite(preferdSiteName));
            return preferdSiteName;
        }
        throw new S3ServerException(S3Error.SCM_CONTENSERER_NO_INSTANCE,
                "no contentserver instance for workspace:ws=" + ws + ", wsSite=" + siteIds
                        + ", discoverySiteCache=" + siteInfos);
    }

    void refreshSiteCache() throws S3ServerException {
        Map<Integer, SiteInfo> newSiteInfos = new ConcurrentHashMap<>();
        List<String> services = discoveryClient.getServices();
        for (String service : services) {
            List<ServiceInstance> instances = discoveryClient.getInstances(service);
            if (instances == null || instances.size() <= 0) {
                continue;
            }
            Map<String, String> metaData = instances.get(0).getMetadata();
            String isContentServer = metaData.getOrDefault("isContentServer", "false");
            if (!isContentServer.equals("true")) {
                continue;
            }
            Set<String> zoneList = new HashSet<>();
            for (ServiceInstance instance : instances) {
                String zone = instance.getMetadata().get("zone");
                if (zone == null) {
                    throw new S3ServerException(S3Error.INTERNAL_ERROR,
                            "contentserver info is invalid in service center, missing zone:"
                                    + instance.getHost() + ":" + instance.getPort() + ", meta="
                                    + instance.getMetadata());
                }
                String region = instance.getMetadata().get("region");
                if (region == null) {
                    throw new S3ServerException(S3Error.INTERNAL_ERROR,
                            "contentserver info is invalid in service center, missing region:"
                                    + instance.getHost() + ":" + instance.getPort() + ", meta="
                                    + instance.getMetadata());
                }
                zoneList.add(region + "-" + zone);
            }

            SiteInfo siteInfo = new SiteInfo();
            siteInfo.setRegion(zoneList);
            siteInfo.setName(service);

            String idStr = metaData.get("siteId");
            if (idStr == null) {
                throw new S3ServerException(S3Error.INTERNAL_ERROR,
                        "contentserver info is invalid in service center, missing siteId:"
                                + service);
            }
            int id = Integer.valueOf(idStr);
            siteInfo.setId(id);

            String isRootSiteStr = metaData.getOrDefault("isRootSiteInstance", "false");
            if (isRootSiteStr.equals("true")) {
                siteInfo.setRoot(true);
                rootSite = siteInfo;
            }

            newSiteInfos.put(siteInfo.getId(), siteInfo);
        }

        siteInfos = newSiteInfos;
    }

    public String getRootSite() throws S3ServerException {
        if (rootSite == null) {
            refreshSiteCache();
            if (rootSite == null) {
                throw new S3ServerException(S3Error.SCM_CONTENSERER_NO_INSTANCE,
                        "root site instance not found");
            }
        }
        return rootSite.getName();
    }

    @PreDestroy
    public void destory() {
        timer.cancel();
        CommonUtil.closeResource(httpClient);
        CommonUtil.closeResource(connectionManager);
    }
}

class SiteInfoRefresher extends ScmTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(SiteInfoRefresher.class);
    private ScmClientFactory factory;

    public SiteInfoRefresher(ScmClientFactory factory) {
        this.factory = factory;
    }

    @Override
    public void run() {
        try {
            factory.refreshSiteCache();
        }
        catch (Exception e) {
            logger.warn("refresh site info failed", e);
        }
    }

}

class WorkspacePreferedSite {
    private String siteName;
    private long time;

    public WorkspacePreferedSite(String siteName) {
        this.siteName = siteName;
        this.time = System.currentTimeMillis();

    }

    public boolean isExpired(long ttl) {
        return System.currentTimeMillis() - time > ttl;
    }

    public String getSiteName() {
        return siteName;
    }
}

class HttpConnectionCleaner extends ScmTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(HttpConnectionCleaner.class);
    private PoolingHttpClientConnectionManager connectionManager;

    public HttpConnectionCleaner(PoolingHttpClientConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void run() {
        if (logger.isDebugEnabled()) {
            Set<HttpRoute> routes = connectionManager.getRoutes();
            for (HttpRoute route : routes) {
                logger.debug("connnection manager info:route={},routeState={}", route,
                        connectionManager.getStats(route));
            }
            logger.debug("connection manager total state:{}", connectionManager.getTotalStats());
        }
        connectionManager.closeExpiredConnections();
    }

}
