package com.sequoiacm.infrastructure.config.client.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PreDestroy;

import com.sequoiacm.infrastructure.config.client.NotifyCallback;
import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.customizer.ConfigCustomizer;
import com.sequoiacm.infrastructure.config.core.customizer.ConfigCustomizerMgr;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.config.client.ScmConfClient;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.msg.ConfigEntityTranslator;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;

@Component
public class ScmConfSubscriberMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfSubscriberMgr.class);

    @Autowired
    private ConfigEntityTranslator configEntityTranslator;

    @Autowired
    private ConfigCustomizerMgr configCustomizerMgr;

    private final ScmTimer timer = ScmTimerFactory.createScmTimer();
    private final Map<String, ScmConfSubscriber> subscribers = new ConcurrentHashMap<>();
    private final Map<String, ConfVersionChecker> confVersionTasks = new ConcurrentHashMap<>();
    private final ReentrantLock notifyLock = new ReentrantLock();

    public void subscribe(String businessType, int heartbeatInterval,
            int initStatusHeartbeatInterval, ScmConfClient scmConfClient,
            NotifyCallback notifyCallback) {
        boolean isNewSubscriber = false;
        ScmConfSubscriber subscriber = subscribers.get(businessType);
        if (subscriber == null) {
            synchronized (this) {
                subscriber = subscribers.get(businessType);
                if (subscriber == null) {
                    subscriber = new ScmConfSubscriber(businessType);
                    subscribers.put(businessType, subscriber);
                    isNewSubscriber = true;
                }
            }
        }
        subscriber.addNotifyCallback(notifyCallback);

        if (!isNewSubscriber) {
            return;
        }

        if (heartbeatInterval == -1) {
            // 不需要用定时器，即允许发生通知丢失问题
            return;
        }

        ConfigCustomizer configCustomizer = configCustomizerMgr.get(businessType);

        VersionFilter versionFilter = new VersionFilter(businessType);
        if (configCustomizer.heartbeatOption().isGlobalVersionHeartbeat()) {
            versionFilter
                    .addBusinessName(configCustomizer.heartbeatOption().getGlobalVersionName());
        }

        if (initStatusHeartbeatInterval > 0) {
            // 临时的检查配置版本任务
            ConfVersionTempChecker tempChecker = new ConfVersionTempChecker(subscriber,
                    versionFilter, scmConfClient, notifyLock, heartbeatInterval);
            timer.schedule(tempChecker, 0, initStatusHeartbeatInterval);
        }

        ConfVersionChecker checker = new ConfVersionChecker(subscriber, versionFilter,
                scmConfClient, notifyLock);
        timer.schedule(checker, heartbeatInterval, heartbeatInterval);
        logger.info("config version's heartbeat is started:businessType={},interval={}",
                businessType,
                heartbeatInterval);
        confVersionTasks.put(businessType, checker);
    }

    @PreDestroy
    public void destroy() {
        timer.cancel();
    }

    @Async
    public Future<String> processNotify(String businessType, EventType type,
            BSONObject notification) {
        try {
            NotifyOption notify = configEntityTranslator.fromNotifyOptionBSON(businessType,
                    notification);
            ScmConfSubscriber p = subscribers.get(businessType);
            if (p == null) {
                logger.error("no such notify processer:configName={},notify={}", businessType,
                        notify);
                return new AsyncResult<>(null);
            }

            notifyLock.lock();
            try {
                boolean isSuccess = p.processNotify(type, notify.getBusinessName(), notify);
                // 有些配置发生变动不会修改版本号
                if (isSuccess && notify.getBusinessVersion() != null) {
                    ConfVersionChecker versionChecker = confVersionTasks.get(businessType);
                    if (versionChecker == null) {
                        logger.error("no such version checker:businessType={}, event={}, notify={}",
                                type, businessType, notify);
                    }
                    else {
                        versionChecker.updateVersion(type, notify.getBusinessVersion());
                    }
                }
            }
            finally {
                notifyLock.unlock();
            }

        }
        catch (Exception e) {
            logger.error(
                    "failed to process notification:configName={},notification={}, eventType={}",
                    businessType, notification, type, e);
        }
        return new AsyncResult<>(null);
    }
}

class ConfVersionTempChecker extends ConfVersionChecker {

    private final long timeToLive;
    private final long startTime;

    public ConfVersionTempChecker(ScmConfSubscriber subscriber, VersionFilter versionFilter,
                                  ScmConfClient client, ReentrantLock notifyLock, long timeToLive) {
        super(subscriber, versionFilter, client, notifyLock);
        this.timeToLive = timeToLive;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        super.run();
        if (System.currentTimeMillis() - startTime > timeToLive) {
            cancel();
        }
    }
}

class ConfVersionChecker extends ScmTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(ConfVersionChecker.class);
    private final ScmConfClient client;
    private final Map<String, Version> myVersions;
    private final ScmConfSubscriber subscriber;
    private final ReentrantLock notifyLock;
    private final VersionFilter versionFilter;

    public ConfVersionChecker(ScmConfSubscriber subscriber, VersionFilter versionFilter,
                              ScmConfClient client, ReentrantLock notifyLock) {
        this.notifyLock = notifyLock;
        this.subscriber = subscriber;
        this.client = client;
        this.myVersions = new ConcurrentHashMap<>();
        this.versionFilter = versionFilter;

        try {
            List<Version> versions = client.getConfVersion(subscriber.getBusinessType(),
                    versionFilter);
            for (Version version : versions) {
                myVersions.put(version.getBusinessName(), version);
            }
        }
        catch (Exception e) {
            logger.warn("failed to query version from config server, try again later..", e);
        }
    }

    @Override
    public void run() {
        try {
            List<Version> latestVersions = client.getConfVersion(subscriber.getBusinessType(),
                    versionFilter);
            List<String> latestVersionNames = new ArrayList<>();
            for (Version latestVersion : latestVersions) {
                latestVersionNames.add(latestVersion.getBusinessName());
                Version myVersion = myVersions.get(latestVersion.getBusinessName());

                boolean isNotifySuccess;
                EventType eventType;
                if (myVersion == null) {

                    eventType = EventType.CREATE;

                }
                else if (latestVersion.getVersion() != myVersion.getVersion()) {
                    eventType = EventType.UPDATE;
                }
                else {
                    // version is same
                    continue;
                }

                notifyLock.lock();
                try {
                    logger.info(
                            "business version incompatible:myVersion={}, latestVersion={}, eventType={}",
                            myVersion, latestVersion, eventType);
                    // 3. 更新 myVersion
                    isNotifySuccess = processNotify(eventType, latestVersion);
                    if (isNotifySuccess) {
                        myVersions.put(latestVersion.getBusinessName(), latestVersion);
                    }
                }
                finally {
                    notifyLock.unlock();
                }
            }

            for (String myVersionName : myVersions.keySet()) {
                if (!latestVersionNames.contains(myVersionName)) {
                    notifyLock.lock();
                    try {
                        logger.info(
                                "business version incompatible:myVersion={}, latestVersion={}, eventType={}",
                                myVersions.get(myVersionName), null, EventType.DELTE);
                        boolean isSuccess = processNotify(EventType.DELTE,
                                myVersions.get(myVersionName));
                        if (isSuccess) {
                            myVersions.remove(myVersionName);
                        }
                    }
                    finally {
                        notifyLock.unlock();
                    }
                }
            }
        }
        catch (Exception e) {
            logger.warn("failed to check version:businessType={},filter={}",
                    subscriber.getBusinessType(), versionFilter, e);
        }
    }

    private boolean processNotify(EventType eventType, Version version) {
        return subscriber.processNotify(eventType, version.getBusinessName(), null);
    }

    public void updateVersion(EventType eventType, Version newVersion) {
        if (eventType == EventType.DELTE) {
            myVersions.remove(newVersion.getBusinessName());
        }
        else {
            if (newVersion.getVersion() != -1) {
                myVersions.put(newVersion.getBusinessName(), newVersion);
            }
        }
    }

}
