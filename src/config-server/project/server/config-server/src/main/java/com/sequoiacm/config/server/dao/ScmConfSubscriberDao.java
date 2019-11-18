package com.sequoiacm.config.server.dao;

import java.util.List;

import com.sequoiacm.config.framework.subscriber.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public interface ScmConfSubscriberDao {
    public void createSubscriber(String configName, String serviceName) throws ScmConfigException;

    public List<ScmConfSubscriber> querySubscribers(String configName) throws ScmConfigException;

    public void deleteSubscriber(String configName, String serviceName) throws ScmConfigException;

}