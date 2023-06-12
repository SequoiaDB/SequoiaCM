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
import com.sequoiacm.infrastructure.config.core.msg.role.RoleUpdater;

@Component
public class UpdateRoleDao {

    private static final Logger logger = LoggerFactory.getLogger(UpdateRoleDao.class);

    public ScmConfOperateResult update(RoleUpdater updator) {
        logger.info("update role success:{}", updator.getName());
        ScmConfEvent event = createEvent(updator.getName());
        RoleConfig roleConfig = new RoleConfig(updator.getName());
        return new ScmConfOperateResult(roleConfig, event);
    }

    private ScmConfEvent createEvent(String username) {
        RoleNotifyOption notifyOption = new RoleNotifyOption(username);
        return new ScmConfEvent(ScmBusinessTypeDefine.ROLE, EventType.UPDATE, notifyOption);
    }
}
