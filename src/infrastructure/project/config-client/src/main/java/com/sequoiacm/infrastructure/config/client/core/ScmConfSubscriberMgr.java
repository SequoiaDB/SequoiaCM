package com.sequoiacm.infrastructure.config.client.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PreDestroy;

import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
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
import com.sequoiacm.infrastructure.config.client.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.BsonConverterMgr;
import com.sequoiacm.infrastructure.config.core.msg.NotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.Version;

@Component
public class ScmConfSubscriberMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfSubscriberMgr.class);

    @Autowired
    private BsonConverterMgr converterMgr;

    private ScmTimer timer = ScmTimerFactory.createScmTimer();
    private Map<String, ScmConfSubscriber> subscribers = new ConcurrentHashMap<>();
    private Map<String, ConfVersionChecker> confVersionTasks = new ConcurrentHashMap<>();
    private ReentrantLock notifyLock = new ReentrantLock();

    public void addSubscriber(ScmConfSubscriber subscriber, ScmConfClient client)
            throws ScmConfigException {
        ScmConfSubscriber old = subscribers.put(subscriber.subscribeConfigName(), subscriber);
        if (old != null) {
            throw new ScmConfigException(ScmConfError.SYSTEM_ERROR,
                    "subscriber already exist for config: " + subscriber.subscribeConfigName()
                            + ", can not subscribe twice");
        }
        addTempChecker(subscriber, client);
        ConfVersionChecker checker = new ConfVersionChecker(subscriber, client, notifyLock);
        timer.schedule(checker, subscriber.getHeartbeatIterval(), subscriber.getHeartbeatIterval());
        logger.info("config version's heartbeat is started:configName={},filter={},interval={}",
                subscriber.subscribeConfigName(), subscriber.getVersionFilter(),
                subscriber.getHeartbeatIterval());
        confVersionTasks.put(subscriber.subscribeConfigName(), checker);
    }

    private void addTempChecker(ScmConfSubscriber subscriber, ScmConfClient client) {
        if (subscriber.getInitStatusInterval() > 0) {
            // 临时的检查配置版本任务
            ConfVersionChecker tempChecker = new ConfVersionChecker(subscriber, client, notifyLock,
                    true);
            timer.schedule(tempChecker, 0, subscriber.getInitStatusInterval());
        }
    }

    @PreDestroy
    public void destory() {
        timer.cancel();
    }

    @Async
    public Future<String> processNotify(String configName, EventType type,
            BSONObject notification) {
        try {
            NotifyOption notify = converterMgr.getMsgConverter(configName)
                    .convertToNotifyOption(type, notification);
            ScmConfSubscriber p = subscribers.get(configName);
            if (p == null) {
                logger.error("no such notify processer:configName={},notify={}", configName,
                        notify);
                return new AsyncResult<>(null);
            }

            notifyLock.lock();
            try {
                p.processNotify(notify);

                // 有些配置发生变动（bucket增加删除）不会修改版本号
                if (notify.getVersion() != null) {
                    confVersionTasks.get(configName).updateVersion(notify.getEventType(),
                            notify.getVersion());
                }
            }
            finally {
                notifyLock.unlock();
            }

        }
        catch (Exception e) {
            logger.error(
                    "failed to process notification:configName={},notification={}, eventType={}",
                    configName, notification, type, e);
        }
        return new AsyncResult<>(null);
    }
}

class ConfVersionChecker extends ScmTimerTask {
    private static final Logger logger = LoggerFactory.getLogger(ConfVersionChecker.class);
    private ScmConfClient client;
    private Map<String, Version> myVersions;
    private ScmConfSubscriber subscriber;
    private ReentrantLock notifyLock;
    private boolean isTemp;
    private long runTime = 0L;

    public ConfVersionChecker(ScmConfSubscriber processer, ScmConfClient client,
            ReentrantLock notifyLock, boolean isTemp) {
        this.notifyLock = notifyLock;
        this.subscriber = processer;
        this.client = client;
        this.myVersions = new ConcurrentHashMap<>();
        this.isTemp = isTemp;
        runTime = System.currentTimeMillis();

        try {
            List<Version> versions = client.getConfVersion(subscriber.subscribeConfigName(),
                    subscriber.getVersionFilter());
            for (Version version : versions) {
                myVersions.put(version.getBussinessName(), version);
            }
        }
        catch (Exception e) {
            logger.warn("failed to query version from config server, try again later..", e);
        }
    }

    public ConfVersionChecker(ScmConfSubscriber processer, ScmConfClient client,
            ReentrantLock notifyLock) {
        this(processer, client, notifyLock, false);
    }

    @Override
    public void run() {
        try {
            List<Version> latestVersions = client.getConfVersion(subscriber.subscribeConfigName(),
                    subscriber.getVersionFilter());
            List<String> latestVersionNames = new ArrayList<>();
            for (Version latestVersion : latestVersions) {
                latestVersionNames.add(latestVersion.getBussinessName());
                Version myVersion = myVersions.get(latestVersion.getBussinessName());

                boolean isNotifySuccess = false;
                NotifyOption notifyOption;
                if (myVersion == null) {
                    notifyOption = subscriber.versionToNotifyOption(EventType.CREATE,
                            latestVersion);
                }
                else if (latestVersion.getVersion() != myVersion.getVersion()) {
                    notifyOption = subscriber.versionToNotifyOption(EventType.UPDATE,
                            latestVersion);
                }
                else {
                    // version is same
                    continue;
                }

                notifyLock.lock();
                try {
                    isNotifySuccess = processNotify(notifyOption);
                    if (isNotifySuccess) {
                        myVersions.put(latestVersion.getBussinessName(), latestVersion);
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
                        NotifyOption notifyOption = subscriber.versionToNotifyOption(
                                EventType.DELTE, myVersions.get(myVersionName));
                        boolean isSuccess = processNotify(notifyOption);
                        if (isSuccess) {
                            myVersions.remove(myVersionName);
                        }
                    }
                    finally {
                        notifyLock.unlock();
                    }
                }
            }

            if (isTemp) {
                if (System.currentTimeMillis() - runTime >= subscriber.getHeartbeatIterval()) {
                    // 如果临时任务执行的时长达到了正式任务的延迟启动时长，就剔除掉该任务。
                    cancel();
                }
            }
        }
        catch (Exception e) {
            logger.warn("failed to check version:configame={},filter={}",
                    subscriber.subscribeConfigName(), subscriber.getVersionFilter(), e);
        }
    }

    private boolean processNotify(NotifyOption notifyOption) {
        try {
            subscriber.processNotify(notifyOption);
            return true;
        }
        catch (Exception e) {
            logger.error("failed process notification:{}", notifyOption, e);
            return false;
        }
    }

    public void updateVersion(EventType eventType, Version newVersion) {
        if (eventType == EventType.DELTE) {
            myVersions.remove(newVersion.getBussinessName());
        }
        else {
            myVersions.put(newVersion.getBussinessName(), newVersion);
        }
    }

}
