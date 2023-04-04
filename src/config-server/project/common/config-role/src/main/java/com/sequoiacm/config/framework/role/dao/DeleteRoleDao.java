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
import com.sequoiacm.infrastructure.config.core.msg.role.RoleFilter;
import com.sequoiacm.infrastructure.config.core.msg.role.RoleNotifyOption;

@Component
public class DeleteRoleDao {

    private static final Logger logger = LoggerFactory.getLogger(DeleteRoleDao.class);

    public ScmConfOperateResult delete(RoleFilter filter) {
        logger.info("delete role success:{}", filter.getName());
        ScmConfEvent event = createEvent(filter.getName());
        RoleConfig roleConfig = (RoleConfig) new RoleBsonConverter()
                .convertToConfig(filter.toBSONObject());
        return new ScmConfOperateResult(roleConfig, event);
    }

    private ScmConfEvent createEvent(String username) {
        RoleNotifyOption notifyOption = new RoleNotifyOption(username, EventType.DELTE);
        return new ScmConfEventBase(ScmConfigNameDefine.ROLE, notifyOption);
    }
}
