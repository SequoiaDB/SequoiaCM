package com.sequoiacm.config.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.config.framework.subscriber.ScmConfSubscriber;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.server.core.ScmConfFrameworkMgr;
import com.sequoiacm.config.server.core.ScmConfigEventMgr;
import com.sequoiacm.config.server.dao.ScmConfSubscriberDao;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.BsonConverterMgr;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;
import com.sequoiacm.infrastructure.config.core.msg.EnableBsonConvertor;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;

@Service
@EnableBsonConvertor
public class ScmConfServiceImpl implements ScmConfService {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfServiceImpl.class);
    @Autowired
    ScmConfFrameworkMgr frameworkMgr;

    @Autowired
    Metasource metasource;

    @Autowired
    ScmConfigEventMgr eventMgr;

    @Autowired
    ScmConfSubscriberDao subscriberDao;

    @Autowired
    BsonConverterMgr convertorMgr;

    @Override
    public Config createConf(String configName, BSONObject configObj, boolean isAsyncNotify)
            throws ScmConfigException {
        ScmConfOperator op = frameworkMgr.getConfOperator(configName);
        Config config = convertorMgr.getMsgConverter(configName).convertToConfig(configObj);
        ScmConfOperateResult res = op.createConf(config);
        return proccessOpResult(res, isAsyncNotify);
    }

    @Override
    public Config deleteConf(String configName, BSONObject filterObj, boolean isAsyncNotify)
            throws ScmConfigException {
        ScmConfOperator op = frameworkMgr.getConfOperator(configName);
        ConfigFilter filter = convertorMgr.getMsgConverter(configName)
                .convertToConfigFilter(filterObj);
        ScmConfOperateResult res = op.deleteConf(filter);
        return proccessOpResult(res, isAsyncNotify);
    }

    @Override
    public Config updateConf(String configName, BSONObject updatorObj, boolean isAsyncNotify)
            throws ScmConfigException {
        ScmConfOperator op = frameworkMgr.getConfOperator(configName);
        ConfigUpdator updator = convertorMgr.getMsgConverter(configName)
                .convertToConfigUpdator(updatorObj);
        ScmConfOperateResult res = op.updateConf(updator);
        return proccessOpResult(res, isAsyncNotify);
    }

    @Override
    public List<Config> getConf(String configName, BSONObject filterObj) throws ScmConfigException {
        ScmConfOperator op = frameworkMgr.getConfOperator(configName);
        ConfigFilter filter = convertorMgr.getMsgConverter(configName)
                .convertToConfigFilter(filterObj);
        return op.getConf(filter);
    }

    @Override
    public List<Version> getConfVersion(String configName, BSONObject filterObj)
            throws ScmConfigException {
        ScmConfOperator op = frameworkMgr.getConfOperator(configName);
        VersionFilter filter = convertorMgr.getMsgConverter(configName)
                .convertToVersionFilter(filterObj);
        return op.getConfVersion(filter);
    }

    @Override
    public void subscribe(String configName, String serviceName) throws ScmConfigException {
        subscriberDao.createSubscriber(configName, serviceName);
    }

    @Override
    public void unsubscribe(String configName, String serviceName) throws ScmConfigException {
        subscriberDao.deleteSubscriber(configName, serviceName);
    }

    private Config proccessOpResult(ScmConfOperateResult res, boolean isAsyncNotify)
            throws ScmConfigException {
        List<ScmConfEvent> events = res.getEvent();
        if (events == null) {
            return res.getConfig();
        }

        List<Future<String>> eventsFuture = new ArrayList<>();
        for (ScmConfEvent event : events) {
            if (event == null) {
                continue;
            }
            Future<String> future = eventMgr.onEvent(event, isAsyncNotify);
            eventsFuture.add(future);
        }

        if (!isAsyncNotify) {
            boolean hasError = false;
            for (int i = 0; i < eventsFuture.size(); i++) {
                try {
                    eventsFuture.get(i).get();
                }
                catch (Exception e) {
                    logger.error("config operate success, but faild to wait event resp:event={}",
                            events.get(i), e);
                    hasError = true;
                }
            }

            if (hasError) {
                throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                        "config operate success, but faild to wait event resp");
            }
        }

        return res.getConfig();
    }

    @Override
    public List<ScmConfSubscriber> listSubsribers() throws ScmConfigException {
        return subscriberDao.querySubscribers(null);
    }
}
