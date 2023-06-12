package com.sequoiacm.config.framework.config.role.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmBusinessTypeDefine;
import com.sequoiacm.infrastructure.config.core.msg.role.RoleConfig;
import com.sequoiacm.infrastructure.config.core.msg.role.RoleNotifyOption;

@Component
public class CreateRoleDao {

    private static final Logger logger = LoggerFactory.getLogger(CreateRoleDao.class);

    public ScmConfOperateResult create(RoleConfig config) {
        logger.info("create role success:{}", config.getName());
        ScmConfEvent event = createEvent(config.getName());
        return new ScmConfOperateResult(config, event);
    }

    private ScmConfEvent createEvent(String username) {
        RoleNotifyOption notifyOption = new RoleNotifyOption(username);
        return new ScmConfEvent(ScmBusinessTypeDefine.ROLE,EventType.CREATE, notifyOption);
    }
}
