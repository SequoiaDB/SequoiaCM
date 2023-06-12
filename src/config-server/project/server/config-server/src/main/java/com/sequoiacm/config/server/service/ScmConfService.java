package com.sequoiacm.config.server.service;

import java.util.List;

import org.bson.BSONObject;

import com.sequoiacm.config.framework.subscriber.ScmConfSubscriber;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.Version;

public interface ScmConfService {
    Config createConf(String businessType, BSONObject config, boolean isAsyncNotify)
            throws ScmConfigException;

    Config deleteConf(String businessType, BSONObject config, boolean isAsyncNotify)
            throws ScmConfigException;

    Config updateConf(String businessType, BSONObject config, boolean isAsyncNotify)
            throws ScmConfigException;

    List<Config> getConf(String businessType, BSONObject option) throws ScmConfigException;

    long countConf(String businessType, BSONObject option) throws ScmConfigException;

    MetaCursor listConf(String businessType, BSONObject option) throws ScmConfigException;

    List<Version> getConfVersion(String businessType, BSONObject option) throws ScmConfigException;

    void subscribe(String businessType, String serviceName) throws ScmConfigException;

    void unsubscribe(String businessType, String serviceName) throws ScmConfigException;

    List<ScmConfSubscriber> listSubsribers() throws ScmConfigException;
}
