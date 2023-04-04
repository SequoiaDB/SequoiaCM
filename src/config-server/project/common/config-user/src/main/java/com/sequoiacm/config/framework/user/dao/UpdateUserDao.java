package com.sequoiacm.config.framework.user.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.event.ScmConfEventBase;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.user.UserBsonConverter;
import com.sequoiacm.infrastructure.config.core.msg.user.UserConfig;
import com.sequoiacm.infrastructure.config.core.msg.user.UserNotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.user.UserUpdator;

@Component
public class UpdateUserDao {

    private static final Logger logger = LoggerFactory.getLogger(UpdateUserDao.class);

    public ScmConfOperateResult update(UserUpdator updator) {
        logger.info("update user success:{}", updator.getUsername());
        ScmConfEvent event = createEvent(updator.getUsername());
        UserConfig userConfig = (UserConfig) new UserBsonConverter()
                .convertToConfig(updator.toBSONObject());
        return new ScmConfOperateResult(userConfig, event);
    }

    private ScmConfEvent createEvent(String username) {
        UserNotifyOption notifyOption = new UserNotifyOption(username, EventType.UPDATE);
        return new ScmConfEventBase(ScmConfigNameDefine.USER, notifyOption);
    }
}
