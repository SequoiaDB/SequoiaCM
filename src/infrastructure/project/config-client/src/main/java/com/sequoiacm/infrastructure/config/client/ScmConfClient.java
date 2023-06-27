package com.sequoiacm.infrastructure.config.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import com.google.common.base.CaseFormat;
import com.sequoiacm.infrastructure.config.client.core.ScmConfSubscriberMgr;
import com.sequoiacm.infrastructure.config.client.props.NumberCheckRule;
import com.sequoiacm.infrastructure.config.client.props.PropInfo;
import com.sequoiacm.infrastructure.config.client.props.ScmConfFileRewriteListener;
import com.sequoiacm.infrastructure.config.client.props.ScmConfPropsScanner;
import com.sequoiacm.infrastructure.config.client.props.ScmPropsExactMatchRule;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.customizer.ConfigCustomizerMgr;
import com.sequoiacm.infrastructure.config.core.msg.ConfigEntityTranslator;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.common.ScmJsonInputStreamCursor;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.config.client.core.ScmConfPropVerifiersMgr;
import com.sequoiacm.infrastructure.config.client.config.ScmConfigPropsModifierFactory;
import com.sequoiacm.infrastructure.config.client.remote.ScmConfFeignClient;
import com.sequoiacm.infrastructure.config.client.remote.ScmConfFeignClientFactory;
import com.sequoiacm.infrastructure.config.client.remote.ScmConfServerExceptionConvertor;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdater;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.verifier.ScmConfigPropVerifier;
import com.sequoiacm.infrastructure.feign.ScmFeignErrorDecoder;

import feign.Response;

@Component
@EnableFeignClients
@EnableAsync
public class ScmConfClient {
    public static final int DEFAULT_HEARTBEAT_INTERVAL = 3 * 60 * 1000;
    private static Logger logger = LoggerFactory.getLogger(ScmConfClient.class);

    private final static ScmFeignErrorDecoder errDecoder = new ScmFeignErrorDecoder(
            new ScmConfServerExceptionConvertor());

    @Autowired
    private ScmConfSubscriberMgr subscriberMgr;

    @Autowired
    private ScmConfFeignClientFactory confFeignClientFactory;

    @Autowired
    private ClientConfig config;


    @Autowired
    private ScmConfPropVerifiersMgr verifiersMgr;

    @Autowired
    private ScmConfigPropsModifierFactory configPropsDaoFactory;

    private ScmTimer asyncSubscribeTimer;

    @Value("${spring.application.name}")
    private String myServiceName;

    @Autowired
    private ConfigEntityTranslator configEntityTranslator;

    @Autowired
    private Environment env;

    @Autowired
    private ScmConfFileRewriteListener confFileRewriter;

    @Autowired
    private ScmConfPropsScanner confPropsScanner;

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private ConfigCustomizerMgr configCustomizerMgr;

    @PreDestroy
    public void destroy() {
        if (asyncSubscribeTimer != null) {
            asyncSubscribeTimer.cancel();
        }
    }

    public void subscribe(String businessType, NotifyCallback callback) throws ScmConfigException {
        int heartbeatInterval = getHeartbeatInterval(businessType);
        subscriberMgr.subscribe(businessType, heartbeatInterval, configCustomizerMgr.get(businessType)
                        .heartbeatOption().getInitStatusHeartbeatInterval(), this,
                callback);
        try {
            logger.info("subscribe {} config notify event ", businessType);
            confFeignClientFactory.getClient().subscribe(businessType, myServiceName);
        }
        catch (Exception e) {
            logger.warn(
                    "failed to subscribe config notification, retry later: businessType={}, myServiceName={}",
                    businessType, myServiceName, e);
            asyncRetry(businessType, config.getSubscribeRetryInterval());
        }
    }

    private int getHeartbeatInterval(String businessType) throws ScmConfigException {
        // 将 businessType 转为驼峰格式，如 meta_data -> metaData
        String businessTypeCamel = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
                businessType);
        String key = "scm.conf.client." + businessTypeCamel + ".heartbeatInterval";

        confPropsScanner.registerConfProps(Collections.singletonMap(new ScmPropsExactMatchRule(key),
                new PropInfo(new NumberCheckRule(conversionService), false)));

        String interval = env.getProperty(key);
        if (interval == null) {
            // 老版本配置，做下兼容
            key = "scm.conf.version." + businessTypeCamel + "Heartbeat";
            interval = env.getProperty(key);
            if (interval == null) {
                return DEFAULT_HEARTBEAT_INTERVAL;
            }
        }

        int ret = Integer.parseInt(interval);
        if (ret <= 0) {
            logger.warn("invalid heartbeat interval:{}={}, use default:{}", key, ret,
                    DEFAULT_HEARTBEAT_INTERVAL);
            confFileRewriter
                    .rewriteConf(Collections.singletonMap(key, DEFAULT_HEARTBEAT_INTERVAL + ""));
            return DEFAULT_HEARTBEAT_INTERVAL;
        }
        return ret;
    }

    private void asyncRetry(String businessType, int retryInterval) {
        if (asyncSubscribeTimer == null) {
            asyncSubscribeTimer = ScmTimerFactory.createScmTimer();
        }
        SubscribeTask subscribeTask = new SubscribeTask(confFeignClientFactory.getClient(),
                businessType, myServiceName);
        asyncSubscribeTimer.schedule(subscribeTask, retryInterval, retryInterval);
    }

    public Config createConf(String businessType, Config config, boolean isAsyncNotify)
            throws ScmConfigException {
        BSONObject configBson = configEntityTranslator.toConfigBSON(config);
        BSONObject resp = null;
        try {
            if (ScmBusinessTypeDefine.WORKSPACE.equals(businessType)) {
                // 只有创建工作区才走新版请求，减少其他创建请求因配置服务版本旧而重发旧版本的请求，减少消耗。
                resp = confFeignClientFactory.getClient().createConfV2(businessType,
                        configBson, isAsyncNotify);
            }
            else {
                resp = confFeignClientFactory.getClient().createConfV1(businessType,
                        configBson, isAsyncNotify);
            }
        }
        catch (ScmConfigException e) {
            if (isOldVersion(e.getError(), e.getMessage())) {
                // 配置服务旧版本情况下，请求会失败，这时需要重新发一次旧版的请求
                resp = confFeignClientFactory.getClient().createConfV1(businessType,
                        configBson, isAsyncNotify);
            }
            else {
                throw e;
            }
        }
        if (resp == null) {
            return null;
        }
        return configEntityTranslator.fromConfigBSON(businessType, resp);
    }

    private boolean isOldVersion(ScmConfError error, String message) {
        return error == ScmConfError.UNKNOWN_ERROR
                && message.equals("Required BSONObject parameter 'config' is not present");
    }

    public List<Version> getConfVersion(String businessType, VersionFilter filter)
            throws ScmConfigException {
        BasicBSONList resp = (BasicBSONList) confFeignClientFactory.getClient()
                .getConfVersion(businessType, filter.toBSONObject());
        if (resp == null) {
            return null;
        }
        ArrayList<Version> versions = new ArrayList<>();
        for (Object versionObj : resp) {
            Version version = new Version((BSONObject) versionObj);
            versions.add(version);
        }
        return versions;
    }

    public Config updateConfig(String businessType, ConfigUpdater updator, boolean isAsyncNotify)
            throws ScmConfigException {
        BSONObject resp = confFeignClientFactory.getClient().updateConf(businessType,
                configEntityTranslator.toConfigUpdaterBSON(updator), isAsyncNotify);
        if (resp == null) {
            return null;
        }
        return configEntityTranslator.fromConfigBSON(businessType, resp);
    }

    public Config deleteConf(String businessType, ConfigFilter filter, boolean isAsyncNotify)
            throws ScmConfigException {
        BSONObject resp = confFeignClientFactory.getClient().deleteConf(businessType,
                configEntityTranslator.toConfigFilterBSON(filter), isAsyncNotify);
        if (resp == null) {
            return null;
        }
        return configEntityTranslator.fromConfigBSON(businessType, resp);
    }

    public Config getOneConf(String businessType, ConfigFilter filter) throws ScmConfigException {
        List<Config> ret = getConf(businessType, filter);
        if (ret == null || ret.size() <= 0) {
            return null;
        }
        if (ret.size() != 1) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "try to get one conf, but return more than one:businessType=" + businessType
                            + ", filter=" + filter + ", ret=" + ret);
        }
        return ret.get(0);
    }

    public List<Config> getConf(String businessType, ConfigFilter filter) throws ScmConfigException {
        BasicBSONList resp = (BasicBSONList) confFeignClientFactory.getClient().getConf(businessType,
                configEntityTranslator.toConfigFilterBSON(filter));
        if (resp == null) {
            return null;
        }
        List<Config> configs = new ArrayList<>();
        for (Object confObj : resp) {
            Config config = configEntityTranslator.fromConfigBSON(businessType, (BSONObject) confObj);
            configs.add(config);
        }
        return configs;
    }

    private void checkResponse(String methodKey, Response response) throws ScmConfigException {
        if (response.status() >= 200 && response.status() < 300) {
            return;
        }

        Exception e = errDecoder.decode(methodKey, response);
        if (e instanceof ScmConfigException) {
            throw (ScmConfigException) e;
        }
        else {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR, e.getMessage(), e);
        }
    }

    public long countConf(String businessType, ConfigFilter filter) throws ScmConfigException {
        Response resp = confFeignClientFactory.getClient().countConf(businessType,
                configEntityTranslator.toConfigFilterBSON(filter));
        checkResponse("count", resp);
        Map<String, Collection<String>> headers = resp.headers();
        Collection<String> countHeaders = headers.get(ScmRestArgDefine.COUNT_HEADER);
        if (countHeaders == null || countHeaders.isEmpty()) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "failed to count conf, response missing count header:confName=" + businessType
                            + ", filter=" + filter);
        }
        return Long.parseLong(countHeaders.iterator().next());
    }

    public ScmJsonInputStreamCursor<Config> listConf(final String businessType, ConfigFilter filter)
            throws ScmConfigException {
        Response resp = confFeignClientFactory.getClient().listConf(businessType,
                configEntityTranslator.toConfigFilterBSON(filter));
        checkResponse("listConf", resp);
        InputStream is;
        try {
            is = resp.body().asInputStream();
            return new ScmJsonInputStreamCursor<Config>(is) {
                @Override
                protected Config convert(BSONObject b) {
                    return configEntityTranslator.fromConfigBSON(businessType, b);
                }
            };
        }
        catch (IOException e) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "failed to list conf:businessType=" + businessType + ", filter=" + filter, e);
        }

    }

    /**
     * First register, higher priority
     *
     * @param verifier
     * @throws ScmConfigException
     */
    public void registerConfigPropVerifier(ScmConfigPropVerifier verifier)
            throws ScmConfigException {
        verifiersMgr.addVerifier(verifier);
    }

    /**
     *
     * @param relativeConfFilePath
     *            ConfFilePath that relative to the config-client jar
     * @throws ScmConfigException
     */
    public void setConfFilePath(String relativeConfFilePath) throws ScmConfigException {
        configPropsDaoFactory.setConfigPropsPath(relativeConfFilePath);
    }

    public String getGlobalConfig(String confName) throws ScmConfigException {
        Map<String, String> map = confFeignClientFactory.getClient().getGlobalConf(confName);
        if (map == null) {
            return null;
        }
        return map.get(confName);
    }
}

class SubscribeTask extends ScmTimerTask {
    private static Logger logger = LoggerFactory.getLogger(SubscribeTask.class);
    private String serviceName;
    private String businessType;
    private ScmConfFeignClient confFeignClient;

    public SubscribeTask(ScmConfFeignClient client, String businessType, String serviceName) {
        this.confFeignClient = client;
        this.businessType = businessType;
        this.serviceName = serviceName;
    }

    @Override
    public void run() {
        try {
            confFeignClient.subscribe(businessType, serviceName);
            logger.info("subscribe config notification success:businessType={},myServiceName={}",
                    businessType, serviceName);
            cancel();
        }
        catch (Exception e) {
            logger.warn(
                    "failed to subscibe config notication, retry later:businessType={},mySeriveName={}",
                    businessType, serviceName, e);
        }

    }

}
