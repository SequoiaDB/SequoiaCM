package com.sequoiacm.config.framework.user.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.event.ScmConfEventBase;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.user.UserConfig;
import com.sequoiacm.infrastructure.config.core.msg.user.UserNotifyOption;

@Component
public class CreateUserDao {

    private static final Logger logger = LoggerFactory.getLogger(CreateUserDao.class);

    public ScmConfOperateResult create(UserConfig config) {
        logger.info("create user success:{}", config.getUsername());
        ScmConfEvent event = createEvent(config.getUsername());
        return new ScmConfOperateResult(config, event);
    }

    private ScmConfEvent createEvent(String username) {
        UserNotifyOption notifyOption = new UserNotifyOption(username, EventType.CREATE);
        return new ScmConfEventBase(ScmConfigNameDefine.USER, notifyOption);
    }
}
