package com.sequoiacm.config.framework.role.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.event.ScmConfEvent;
import com.sequoiacm.config.framework.event.ScmConfEventBase;
import com.sequoiacm.config.framework.operator.ScmConfOperateResult;
import com.sequoiacm.infrastructure.config.core.common.EventType;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.msg.role.RoleBsonConverter;
import com.sequoiacm.infrastructure.config.core.msg.role.RoleConfig;
import com.sequoiacm.infrastructure.config.core.msg.role.RoleNotifyOption;
import com.sequoiacm.infrastructure.config.core.msg.role.RoleUpdator;

@Component
public class UpdateRoleDao {

    private static final Logger logger = LoggerFactory.getLogger(UpdateRoleDao.class);

    public ScmConfOperateResult update(RoleUpdator updator) {
        logger.info("update role success:{}", updator.getName());
        ScmConfEvent event = createEvent(updator.getName());
        RoleConfig roleConfig = (RoleConfig) new RoleBsonConverter()
                .convertToConfig(updator.toBSONObject());
        return new ScmConfOperateResult(roleConfig, event);
    }

    private ScmConfEvent createEvent(String username) {
        RoleNotifyOption notifyOption = new RoleNotifyOption(username, EventType.UPDATE);
        return new ScmConfEventBase(ScmConfigNameDefine.ROLE, notifyOption);
    }
}
