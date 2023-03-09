package com.sequoiacm.infrastructure.config.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.common.ScmJsonInputStreamCursor;
import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.config.client.core.ScmConfPropVerifiersMgr;
import com.sequoiacm.infrastructure.config.client.core.ScmConfSubscriberMgr;
import com.sequoiacm.infrastructure.config.client.dao.ScmConfigPropsDaoFactory;
import com.sequoiacm.infrastructure.config.client.remote.ScmConfFeignClient;
import com.sequoiacm.infrastructure.config.client.remote.ScmConfFeignClientFactory;
import com.sequoiacm.infrastructure.config.client.remote.ScmConfServerExceptionConvertor;
import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.BsonConverterMgr;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.EnableBsonConvertor;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import com.sequoiacm.infrastructure.config.core.verifier.ScmConfigPropVerifier;
import com.sequoiacm.infrastructure.feign.ScmFeignErrorDecoder;

import feign.Response;

@Component
@EnableBsonConvertor
public class ScmConfClient {
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
    private BsonConverterMgr converterMgr;

    @Autowired
    private ScmConfPropVerifiersMgr verifiersMgr;

    @Autowired
    private ScmConfigPropsDaoFactory configPropsDaoFactory;

    private ScmTimer asyncSubscribeTimer;

    @PreDestroy
    public void destory() {
        if (asyncSubscribeTimer != null) {
            asyncSubscribeTimer.cancel();
        }
    }

    public void subscribeWithAsyncRetry(ScmConfSubscriber subscriber) throws ScmConfigException {
        subscriberMgr.addSubscriber(subscriber, this);

        try {
            logger.info("server subscribe {} config notify event ",
                    subscriber.subscribeConfigName());
            confFeignClientFactory.getClient().subscribe(subscriber.subscribeConfigName(),
                    subscriber.myServiceName());
        }
        catch (Exception e) {
            logger.warn(
                    "failed to subscibe config notication, retry later:configName={},mySeriveName={}",
                    subscriber.subscribeConfigName(), subscriber.myServiceName(), e);
            asnycRetry(subscriber.subscribeConfigName(), subscriber.myServiceName(),
                    config.getSubscribeRetryInterval());
        }
    }

    private void asnycRetry(String configName, String serviceName, int retryInterval) {
        if (asyncSubscribeTimer == null) {
            asyncSubscribeTimer = ScmTimerFactory.createScmTimer();
        }
        SubscribeTask subscribeTask = new SubscribeTask(confFeignClientFactory.getClient(),
                configName, serviceName);
        asyncSubscribeTimer.schedule(subscribeTask, retryInterval, retryInterval);
    }

    public void unsubscribe(String configName, String serviceName) throws ScmConfigException {
        confFeignClientFactory.getClient().unsubscribe(configName, serviceName);
    }

    public Config createConf(String configName, Config config, boolean isAsyncNotify)
            throws ScmConfigException {
        BSONObject resp = confFeignClientFactory.getClient().createConf(configName,
                config.toBSONObject(), isAsyncNotify);
        if (resp == null) {
            return null;
        }
        return converterMgr.getMsgConverter(configName).convertToConfig(resp);
    }

    public List<Version> getConfVersion(String configName, VersionFilter filter)
            throws ScmConfigException {
        BasicBSONList resp = (BasicBSONList) confFeignClientFactory.getClient()
                .getConfVersion(configName, filter.toBSONObject());
        if (resp == null) {
            return null;
        }
        ArrayList<Version> versions = new ArrayList<>();
        for (Object configObj : resp) {
            Version version = converterMgr.getMsgConverter(configName)
                    .convertToVersion((BSONObject) configObj);
            versions.add(version);
        }
        return versions;
    }

    public Config updateConfig(String configName, ConfigUpdator updator, boolean isAsyncNotify)
            throws ScmConfigException {
        BSONObject resp = confFeignClientFactory.getClient().updateConf(configName,
                updator.toBSONObject(), isAsyncNotify);
        if (resp == null) {
            return null;
        }
        return converterMgr.getMsgConverter(configName).convertToConfig(resp);
    }

    public Config deleteConf(String configName, ConfigFilter filter, boolean isAsyncNotify)
            throws ScmConfigException {
        BSONObject resp = confFeignClientFactory.getClient().deleteConf(configName,
                filter.toBSONObject(), isAsyncNotify);
        if (resp == null) {
            return null;
        }
        return converterMgr.getMsgConverter(configName).convertToConfig(resp);
    }

    public Config getOneConf(String configName, ConfigFilter filter) throws ScmConfigException {
        List<Config> ret = getConf(configName, filter);
        if (ret == null || ret.size() <= 0) {
            return null;
        }
        if (ret.size() != 1) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "try to get one conf, but return more than one:configName=" + configName
                            + ", filter=" + filter + ", ret=" + ret);
        }
        return ret.get(0);
    }

    public List<Config> getConf(String configName, ConfigFilter filter) throws ScmConfigException {
        BasicBSONList resp = (BasicBSONList) confFeignClientFactory.getClient().getConf(configName,
                filter.toBSONObject());
        if (resp == null) {
            return null;
        }
        List<Config> configs = new ArrayList<>();
        for (Object confObj : resp) {
            Config config = converterMgr.getMsgConverter(configName)
                    .convertToConfig((BSONObject) confObj);
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

    public long countConf(String configName, ConfigFilter filter) throws ScmConfigException {
        Response resp = confFeignClientFactory.getClient().countConf(configName,
                filter.toBSONObject());
        checkResponse("count", resp);
        Map<String, Collection<String>> headers = resp.headers();
        Collection<String> countHeaders = headers.get(ScmRestArgDefine.COUNT_HEADER);
        if (countHeaders == null || countHeaders.isEmpty()) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "failed to count conf, response missing count header:confName=" + configName
                            + ", filter=" + filter);
        }
        return Long.parseLong(countHeaders.iterator().next());
    }

    public ScmJsonInputStreamCursor<Config> listConf(final String configName, ConfigFilter filter)
            throws ScmConfigException {
        Response resp = confFeignClientFactory.getClient().listConf(configName,
                filter.toBSONObject());
        checkResponse("listConf", resp);
        InputStream is;
        try {
            is = resp.body().asInputStream();
            return new ScmJsonInputStreamCursor<Config>(is) {
                @Override
                protected Config convert(BSONObject b) {
                    Config config = converterMgr.getMsgConverter(configName)
                            .convertToConfig((BSONObject) b);
                    return config;
                }
            };
        }
        catch (IOException e) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "failed to list conf:configName=" + configName + ", filter=" + filter, e);
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
}

class SubscribeTask extends ScmTimerTask {
    private static Logger logger = LoggerFactory.getLogger(SubscribeTask.class);
    private String serviceName;
    private String configName;
    private ScmConfFeignClient confFeignClient;

    public SubscribeTask(ScmConfFeignClient client, String configName, String serviceName) {
        this.confFeignClient = client;
        this.configName = configName;
        this.serviceName = serviceName;
    }

    @Override
    public void run() {
        try {
            confFeignClient.subscribe(configName, serviceName);
            logger.info("subscribe config notification success:configName={},myServiceName={}",
                    configName, serviceName);
            cancel();
        }
        catch (Exception e) {
            logger.warn(
                    "failed to subscibe config notication, retry later:configName={},mySeriveName={}",
                    configName, serviceName, e);
        }

    }

}
