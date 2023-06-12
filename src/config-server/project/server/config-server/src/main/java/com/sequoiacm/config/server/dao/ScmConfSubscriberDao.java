package com.sequoiacm.config.server.dao;

import java.util.List;

import com.sequoiacm.config.framework.subscriber.ScmConfSubscriber;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public interface ScmConfSubscriberDao {
    public void createSubscriber(String businessType, String serviceName) throws ScmConfigException;

    public List<ScmConfSubscriber> querySubscribers(String businessType) throws ScmConfigException;

    public void deleteSubscriber(String businessType, String serviceName) throws ScmConfigException;

}