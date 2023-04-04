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
import com.sequoiacm.infrastructure.config.core.msg.user.UserFilter;
import com.sequoiacm.infrastructure.config.core.msg.user.UserNotifyOption;

@Component
public class DeleteUserDao {

    private static final Logger logger = LoggerFactory.getLogger(DeleteUserDao.class);

    public ScmConfOperateResult delete(UserFilter filter) {
        logger.info("delete user success:{}", filter.getUsername());
        ScmConfEvent event = createEvent(filter.getUsername());
        UserConfig userConfig = (UserConfig) new UserBsonConverter()
                .convertToConfig(filter.toBSONObject());
        return new ScmConfOperateResult(userConfig, event);
    }

    private ScmConfEvent createEvent(String username) {
        UserNotifyOption notifyOption = new UserNotifyOption(username, EventType.DELTE);
        return new ScmConfEventBase(ScmConfigNameDefine.USER, notifyOption);
    }
}
