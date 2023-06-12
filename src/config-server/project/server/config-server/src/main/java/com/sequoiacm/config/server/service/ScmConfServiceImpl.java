package com.sequoiacm.config.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.sequoiacm.infrastructure.config.core.msg.ConfigEntityTranslator;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.config.framework.operator.ScmConfOperator;
import com.sequoiacm.config.framework.subscriber.ScmConfSubscriber;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.config.metasource.Metasource;
import com.sequoiacm.config.server.core.ScmConfOperatorMgr;
import com.sequoiacm.config.server.core.ScmConfigEventMgr;
import com.sequoiacm.config.server.dao.ScmConfSubscriberDao;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdater;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;

@Service
public class ScmConfServiceImpl implements ScmConfService {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfServiceImpl.class);
    @Autowired
    ScmConfOperatorMgr operatorMgr;

    @Autowired
    Metasource metasource;

    @Autowired
    ScmConfigEventMgr eventMgr;

    @Autowired
    ScmConfSubscriberDao subscriberDao;

    @Autowired
    private ConfigEntityTranslator configEntityTranslator;

    @Override
    public Config createConf(String businessType, BSONObject configObj, boolean isAsyncNotify)
            throws ScmConfigException {
        ScmConfOperator op = operatorMgr.getConfOperator(businessType);
        Config config = configEntityTranslator.fromConfigBSON(businessType, configObj);
        ScmConfOperateResult res = op.createConf(config);
        return proccessOpResult(res, isAsyncNotify);
    }

    @Override
    public Config deleteConf(String businessType, BSONObject filterObj, boolean isAsyncNotify)
            throws ScmConfigException {
        ScmConfOperator op = operatorMgr.getConfOperator(businessType);
        ConfigFilter filter = configEntityTranslator.fromConfigFilterBSON(businessType, filterObj);
        ScmConfOperateResult res = op.deleteConf(filter);
        return proccessOpResult(res, isAsyncNotify);
    }

    @Override
    public Config updateConf(String businessType, BSONObject updatorObj, boolean isAsyncNotify)
            throws ScmConfigException {
        ScmConfOperator op = operatorMgr.getConfOperator(businessType);
        ConfigUpdater updator = configEntityTranslator.fromConfigUpdaterBSON(businessType,
                updatorObj);
        ScmConfOperateResult res = op.updateConf(updator);
        return proccessOpResult(res, isAsyncNotify);
    }

    @Override
    public List<Config> getConf(String businessType, BSONObject filterObj) throws ScmConfigException {
        ScmConfOperator op = operatorMgr.getConfOperator(businessType);
        ConfigFilter filter = configEntityTranslator.fromConfigFilterBSON(businessType, filterObj);
        return op.getConf(filter);
    }

    @Override
    public long countConf(String businessType, BSONObject filterObj) throws ScmConfigException {
        ScmConfOperator op = operatorMgr.getConfOperator(businessType);
        ConfigFilter filter = configEntityTranslator.fromConfigFilterBSON(businessType, filterObj);
        return op.countConf(filter);
    }

    @Override
    public MetaCursor listConf(String businessType, BSONObject filterObj) throws ScmConfigException {
        ScmConfOperator op = operatorMgr.getConfOperator(businessType);
        ConfigFilter filter = configEntityTranslator.fromConfigFilterBSON(businessType, filterObj);
        return op.listConf(filter);
    }

    @Override
    public List<Version> getConfVersion(String businessType, BSONObject filterObj)
            throws ScmConfigException {
        ScmConfOperator op = operatorMgr.getConfOperator(businessType);
        VersionFilter filter = new VersionFilter(filterObj);
        return op.getConfVersion(filter);
    }

    @Override
    public void subscribe(String businessType, String serviceName) throws ScmConfigException {
        subscriberDao.createSubscriber(businessType, serviceName);
    }

    @Override
    public void unsubscribe(String businessType, String serviceName) throws ScmConfigException {
        subscriberDao.deleteSubscriber(businessType, serviceName);
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
