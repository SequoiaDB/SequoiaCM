package com.sequoiacm.infrastructure.config.client.service;

import java.util.concurrent.Future;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.infrastructure.config.client.core.ScmConfSubscriberMgr;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

@Service
public class ScmConfigNotifyServiceImpl implements ScmConfigNotifyService {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfigNotifyServiceImpl.class);
    @Autowired
    ScmConfSubscriberMgr subscriberMgr;

    @Override
    public void notify(String businessType, EventType type, BSONObject notifyOption,
            boolean isAsyncNotify) throws ScmConfigException {
        Future<String> future = subscriberMgr.processNotify(businessType, type, notifyOption);
        if (!isAsyncNotify) {
            try {
                future.get();
            }
            catch (Exception e) {
                logger.error(
                        "failed to wait for notification process to complete:businessType={},eventType={},notification={}",
                        businessType, type, notifyOption, e);
            }
        }
    }

}
