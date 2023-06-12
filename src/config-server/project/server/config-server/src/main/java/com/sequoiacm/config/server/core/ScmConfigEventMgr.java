package com.sequoiacm.config.server.core;

import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.subscriber.ScmConfSubscriber;
import com.sequoiacm.config.server.dao.ScmConfSubscriberDao;

@Component
public class ScmConfigEventMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfigEventMgr.class);

    @Autowired
    ScmConfigNotifier configNotifier;
    @Autowired
    ScmConfSubscriberDao subscriberDao;

    @Async
    public Future<String> onEvent(ScmConfEvent event, boolean isAsyncNotify) {
        try {
            //查出来就是需要通知的，外面不用再处理
            List<ScmConfSubscriber> listeners = subscriberDao.querySubscribers(event.getBusinessType());
            if (listeners.size() > 0) {
                configNotifier.notifyServices(listeners, event, isAsyncNotify);
            }
        }
        catch (Exception e) {
            logger.error("failed to notify listeners:event={}", event, e);
        }
        return new AsyncResult<>(null);
    }

}
