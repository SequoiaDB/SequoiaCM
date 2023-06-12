package com.sequoiacm.infrastructure.config.client;

import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ScmConfSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfSubscriber.class);
    private volatile List<NotifyCallback> notifyCallbacks = new ArrayList<>();
    private final String businessType;

    public ScmConfSubscriber(String businessType) {
        this.businessType = businessType;
    }

    public synchronized void addNotifyCallback(NotifyCallback callback) {
        ArrayList<NotifyCallback> newNotifyCallbacks = new ArrayList<>(notifyCallbacks);
        newNotifyCallbacks.add(callback);
        newNotifyCallbacks.sort((o1, o2) -> Integer.compare(o1.priority(), o2.priority()));
        notifyCallbacks = newNotifyCallbacks;
    }

    public boolean processNotify(EventType type, String businessName, NotifyOption notification) {
        logger.info("processing notify: type={}, businessName={}, notifyOption={}", type,
                businessName, notification);
        boolean isAllSuccess = true;
        for (NotifyCallback callback : notifyCallbacks) {
            logger.info("processing notify callback: callback={}", callback.getClass());
            try {
                callback.processNotify(type, businessName, notification);
            }
            catch (Exception e) {
                logger.warn(
                        "failed to invoke notify callback: type={}, businessName={}, notifyOption={}, callback={}",
                        type, businessName, notification, callback.getClass(), e);
                isAllSuccess = false;
            }
        }
        logger.info("process notify done");
        return isAllSuccess;
    }


    public String getBusinessType() {
        return businessType;
    }
}
