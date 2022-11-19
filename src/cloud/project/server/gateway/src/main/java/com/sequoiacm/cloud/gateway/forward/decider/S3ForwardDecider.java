package com.sequoiacm.cloud.gateway.forward.decider;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.config.client.core.bucket.BucketConfSubscriber;
import com.sequoiacm.infrastructure.config.client.core.bucket.EnableBucketSubscriber;
import com.sequoiacm.infrastructure.config.client.core.workspace.EnableWorkspaceSubscriber;
import com.sequoiacm.infrastructure.config.client.core.workspace.WorkspaceConfSubscriber;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.bucket.BucketConfig;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceConfig;
import com.sequoiacm.infrastructure.discovery.EnableScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.discovery.InstanceMetaDataDefine;
import com.sequoiacm.infrastructure.discovery.ScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.discovery.ScmServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;


@Configuration
@ConfigurationProperties("scm.s3")
class S3ForwardDeciderConfig {
    private static final Logger logger = LoggerFactory.getLogger(S3ForwardDeciderConfig.class);

    private List<String> userAgent = Collections.singletonList("aws-sdk-java");
    private long chooserRefreshInterval = 180000;

    public void setUserAgent(List<String> s3UserAgentList) {
        List<String> toLowercase = new ArrayList<>();
        for (String src : s3UserAgentList) {
            toLowercase.add(src.toLowerCase());
        }
        this.userAgent = toLowercase;
        logger.info("s3 user agent: {}", this.userAgent);
    }

    public List<String> getUserAgent() {
        return userAgent;
    }

    public long getChooserRefreshInterval() {
        return chooserRefreshInterval;
    }

    public void setChooserRefreshInterval(long chooserRefreshInterval) {
        this.chooserRefreshInterval = chooserRefreshInterval;
    }
}

@EnableBucketSubscriber
@EnableWorkspaceSubscriber
@Component
@EnableScmServiceDiscoveryClient
@Order(S3ForwardDecider.ORDER)
public class S3ForwardDecider implements ForwardDecider {

    public static final int ORDER = ZuulPrefixForwardDecider.ORDER + 1;

    public static final String S3_REQUEST_MARKER_ATTRIBUTE = S3ForwardDecider.class.getName()
            + ".S3_REQUEST_MARKER";
    private static final Logger logger = LoggerFactory.getLogger(S3ForwardDecider.class);
    public static final String S3_AUTHORIZATION_HEADER = "Authorization";
    public static final String S3_AWS_ACCESS_KEY_ID_QUERY_PARAM = "AWSAccessKeyId=";
    public static final String S3_X_AMZ_SIGNATURE_QUERY_PARAM = "X-Amz-Signature=";
    public static final String USER_AGENT_HEADER = "user-agent";
    public static final String X_SCM_PREFERRED_HEADER = "x-scm-preferred";
    private final ZoneRuleChooser zoneRuleS3Chooser;
    private final SiteRuleChooser siteRuleS3Chooser;
    private final ScmTimer timer;

    @Autowired
    private BucketConfSubscriber bucketConfSubscriber;

    @Autowired
    private WorkspaceConfSubscriber workspaceSubscriber;

    private S3ForwardDeciderConfig s3Config;

    @Autowired
    public S3ForwardDecider(ScmServiceDiscoveryClient discoveryClient,
            S3ForwardDeciderConfig s3Config, EurekaInstanceConfigBean eurekaInstanceConfigBean) {
        try {
            this.s3Config = s3Config;
            this.zoneRuleS3Chooser = new ZoneRuleChooser(discoveryClient,
                    eurekaInstanceConfigBean.getHostname());
            this.siteRuleS3Chooser = new SiteRuleChooser(discoveryClient);
            this.timer = ScmTimerFactory.createScmTimer("s3-forward-chooser-refresher");
            timer.schedule(new ScmTimerTask() {
                @Override
                public void run() {
                    logger.debug("refreshing s3 chooser...");
                    try {
                        zoneRuleS3Chooser.refresh();
                    }
                    catch (Exception e) {
                        logger.warn("failed to refresh zone rule chooser", e);
                    }

                    try {
                        siteRuleS3Chooser.refresh();
                    }
                    catch (Exception e) {
                        logger.warn("failed to refresh site rule chooser", e);
                    }
                }
            }, 0, s3Config.getChooserRefreshInterval());
        }
        catch (Exception e) {
            destroy();
            throw e;
        }
    }

    @PreDestroy
    public void destroy() {
        if (timer != null) {
            timer.cancel();
        }
    }
    @Override
    public Decision decide(HttpServletRequest req) {
        if (!isS3Request(req)) {
            return Decision.unrecognized();
        }
        req.setAttribute(S3_REQUEST_MARKER_ATTRIBUTE, S3_REQUEST_MARKER_ATTRIBUTE);
        String targetApi = req.getRequestURI();
        logger.debug("s3 request, choosing s3 service name to forward... : {}", targetApi);
        String s3Service = chooseS3ServiceName(req);
        return Decision.shouldCustomForward(s3Service, targetApi, null, false, false);
    }

    private boolean isS3Request(HttpServletRequest req) {
        if (req.getHeader(S3_AUTHORIZATION_HEADER) != null) {
            logger.debug("the request contain {} header, assume s3 request",
                    S3_AUTHORIZATION_HEADER);
            return true;
        }

        if (req.getQueryString() != null) {
            if (req.getQueryString().contains(S3_AWS_ACCESS_KEY_ID_QUERY_PARAM)
                    || req.getQueryString().contains(S3_X_AMZ_SIGNATURE_QUERY_PARAM)) {
                logger.debug("the request query parameters contain {} or {}, assume s3 request",
                        S3_AWS_ACCESS_KEY_ID_QUERY_PARAM, S3_X_AMZ_SIGNATURE_QUERY_PARAM);
                return true;
            }
        }

        String userAgent = req.getHeader(USER_AGENT_HEADER);
        if (userAgent != null) {
            userAgent = userAgent.toLowerCase();
            for (String s3UserAgent : s3Config.getUserAgent()) {
                if (userAgent.contains(s3UserAgent)) {
                    logger.debug("the request user-agent header contain {}, assume s3 request",
                            s3UserAgent);
                    return true;
                }
            }
        }
        return false;
    }

    private String chooseS3ServiceName(HttpServletRequest req) {
        String preferredSite = req.getHeader(X_SCM_PREFERRED_HEADER);
        if (preferredSite != null) {
            logger.debug("s3 request contain preferred header, using preferred site: {}",
                    preferredSite);
            return siteRuleS3Chooser.chooseS3ServiceBySite(preferredSite);
        }

        String targetApi = req.getRequestURI();
        if (targetApi == null || targetApi.length() <= 0 || targetApi.equals("/")
                || !targetApi.startsWith("/")) {
            logger.debug(
                    "can not detect the bucket name of s3 request, using zone rule to choose s3 service");
            return zoneRuleS3Chooser.chooseS3Service();
        }

        // targetApi = /bucketName/** or /bucketName
        int secondDelimiterIdx = targetApi.indexOf("/", 1);
        String bucket;
        if (secondDelimiterIdx == -1) {
            // targetApi = /bucketName
            bucket = targetApi.substring(1);
        }
        else {
            // targetApi = /bucketName/**
            bucket = targetApi.substring(1, secondDelimiterIdx);
        }

        // targetApi = /bucketName or /bucketName/
        if (bucket.length() + 2 >= targetApi.length()) {
            if (req.getMethod().equals("PUT") && req.getQueryString() != null
                    && req.getQueryString().trim().length() != 0) {
                logger.debug("create bucket request, using zone rule to choose s3 service");
                return zoneRuleS3Chooser.chooseS3Service();
            }
        }

        logger.debug("detect the bucket name of s3 request is {}", bucket);
        return chooseS3ServiceNameByBucket(bucket);
    }

    private String chooseS3ServiceNameByBucket(String bucket) {
        try {
            BucketConfig bucketConf = bucketConfSubscriber.getBucket(bucket);
            if (bucketConf == null) {
                logger.debug(
                        "the bucket not exist, using zone rule to choose s3 service: bucket={}",
                        bucket);
                return zoneRuleS3Chooser.chooseS3Service();
            }
            WorkspaceConfig wsConf = workspaceSubscriber.getWorkspace(bucketConf.getWorkspace());
            if (wsConf == null) {
                logger.debug(
                        "the bucket workspace not exist, using zone rule to choose s3 service: bucket={}, workspace={}",
                        bucket, bucketConf.getWorkspace());
                return zoneRuleS3Chooser.chooseS3Service();
            }

            String preferredSite = wsConf.getPreferred();
            if (preferredSite == null) {
                logger.debug(
                        "workspace preferred is null, forward to root site s3 service: bucket={}, workspace={}",
                        bucket, bucketConf.getWorkspace());
                return siteRuleS3Chooser.chooseS3ServiceByRootSite();
            }
            logger.debug(
                    "workspace preferred is {}, forward to the specified site s3 service: bucket={}, workspace={}",
                    preferredSite, bucket, bucketConf.getWorkspace());
            return siteRuleS3Chooser.chooseS3ServiceBySite(preferredSite);
        }
        catch (ScmConfigException e) {
            throw new IllegalArgumentException(
                    "failed to choose s3 service, get bucket info failed:" + bucket, e);
        }
    }

    @Override
    public String toString() {
        return "S3ForwardDecider";
    }
}

class SiteRuleChooser {
    private final ScmServiceDiscoveryClient discoveryClient;
    private volatile String rootSite;
    private volatile Map<String, String> siteToS3 = Collections.emptyMap();

    public SiteRuleChooser(ScmServiceDiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public String chooseS3ServiceBySite(String siteName) {
        siteName = siteName.toLowerCase();
        String s3 = siteToS3.get(siteName);
        if (s3 == null) {
            refresh();
            s3 = siteToS3.get(siteName);
            if (s3 == null) {
                throw new IllegalArgumentException("the specified site s3 service not found: site="
                        + siteName + ", siteToS3Map=" + siteToS3);
            }
        }
        return s3;
    }

    public String chooseS3ServiceByRootSite() {
        if (rootSite == null) {
            refresh();
            if (rootSite == null) {
                throw new IllegalArgumentException(
                        "failed to get the s3 service of root site, root site not found");
            }
        }
        return chooseS3ServiceBySite(rootSite);
    }

    public void refresh() {
        Map<String, String> latestSiteToS3 = new HashMap<>();
        List<ScmServiceInstance> instances = discoveryClient.getInstances();
        for (ScmServiceInstance instance : instances) {
            Map<String, String> metadata = instance.getMetadata();
            if (metadata == null) {
                continue;
            }

            String isRootSiteInstance = metadata
                    .getOrDefault(InstanceMetaDataDefine.IS_ROOT_SITE_INSTANCE, "false");
            if (Boolean.parseBoolean(isRootSiteInstance)) {
                rootSite = instance.getServiceName();
                continue;
            }

            String isS3Instance = metadata.getOrDefault(InstanceMetaDataDefine.IS_S3_SERVER,
                    "false");
            if (!Boolean.parseBoolean(isS3Instance)) {
                continue;
            }
            String bindingSite = metadata.get(InstanceMetaDataDefine.BINDING_SITE);
            if (bindingSite == null) {
                continue;
            }
            latestSiteToS3.put(bindingSite.toLowerCase(), instance.getServiceName());
        }
        this.siteToS3 = latestSiteToS3;
    }
}

class ZoneRuleChooser {
    private static final Logger logger = LoggerFactory.getLogger(ZoneRuleChooser.class);
    private final ScmServiceDiscoveryClient discoveryClient;
    private final String myHostName;
    private volatile List<String> allS3ServiceList = Collections.emptyList();
    private volatile List<String> myRegionS3ServiceList = Collections.emptyList();
    private volatile List<String> myZoneS3ServiceList = Collections.emptyList();
    private volatile List<String> myHostAndMyZoneS3ServiceList = Collections.emptyList();

    public ZoneRuleChooser(ScmServiceDiscoveryClient discoveryClient, String myHostName) {
        this.discoveryClient = discoveryClient;
        this.myHostName = myHostName;
    }

    public String chooseS3Service() {
        String s3 = innerChooseS3Service();
        if (s3 == null) {
            refresh();
            s3 = innerChooseS3Service();
            if (s3 == null) {
                throw new IllegalArgumentException("s3 service not found");
            }
        }
        return s3;
    }

    private String innerChooseS3Service() {
        if (myHostAndMyZoneS3ServiceList.size() > 0) {
            logger.debug("choose s3 service in my host: s3ServiceList={}", myHostAndMyZoneS3ServiceList);
            return getRandomElement(myHostAndMyZoneS3ServiceList);
        }

        if (myZoneS3ServiceList.size() > 0) {
            logger.debug("choose s3 service in my zone: s3ServiceList={}, myZone={}",
                    myZoneS3ServiceList, discoveryClient.getLocalZone());
            return getRandomElement(myZoneS3ServiceList);
        }

        if (myRegionS3ServiceList.size() > 0) {
            logger.debug("choose s3 service in my region: s3ServiceList={}, myRegion={}",
                    myRegionS3ServiceList, discoveryClient.getLocalRegion());
            return getRandomElement(myRegionS3ServiceList);
        }

        if (allS3ServiceList.size() > 0) {
            logger.debug("choose s3 service in all s3 services: s3ServiceList={}",
                    allS3ServiceList);
            return getRandomElement(allS3ServiceList);
        }

        return null;
    }

    private String getRandomElement(List<String> list) {
        ThreadLocalRandom rd = ThreadLocalRandom.current();
        return list.get(rd.nextInt(list.size()));
    }

    public void refresh() {
        List<ScmServiceInstance> instances = discoveryClient.getInstances();
        Set<String> allS3ServiceSet = new HashSet<>();
        Set<String> myRegionS3ServiceSet = new HashSet<>();
        Set<String> myZoneS3ServiceSet = new HashSet<>();
        Set<String> myHostS3ServiceSet = new HashSet<>();
        for (ScmServiceInstance instance : instances) {
            Map<String, String> metadata = instance.getMetadata();
            if (metadata == null) {
                continue;
            }
            String isS3Instance = metadata.getOrDefault(InstanceMetaDataDefine.IS_S3_SERVER,
                    "false");
            if (!Boolean.parseBoolean(isS3Instance)) {
                continue;
            }

            allS3ServiceSet.add(instance.getServiceName());
            if (discoveryClient.getLocalRegion().equals(instance.getRegion())) {
                myRegionS3ServiceSet.add(instance.getServiceName());
                if (discoveryClient.getLocalZone().equals(instance.getZone())) {
                    myZoneS3ServiceSet.add(instance.getServiceName());
                    if (instance.getHost().equals(myHostName)) {
                        myHostS3ServiceSet.add(instance.getServiceName());
                    }
                }
            }
        }

        allS3ServiceList = new ArrayList<>(allS3ServiceSet);
        myRegionS3ServiceList = new ArrayList<>(myRegionS3ServiceSet);
        myZoneS3ServiceList = new ArrayList<>(myZoneS3ServiceSet);
        myHostAndMyZoneS3ServiceList = new ArrayList<>(myHostS3ServiceSet);
    }
}
