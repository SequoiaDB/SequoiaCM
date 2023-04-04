package com.sequoiacm.infrastructure.config.core.msg.role;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigUpdator;

public class RoleUpdator implements ConfigUpdator {

    private String name;

    public RoleUpdator() {

    }

    public RoleUpdator(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public BSONObject toBSONObject() {
        BSONObject obj = new BasicBSONObject();
        obj.put(ScmRestArgDefine.ROLE_CONF_ROLENAME, name);
        BSONObject updator = new BasicBSONObject();
        updator.put(ScmRestArgDefine.ROLE_CONF_ROLENAME, name);
        obj.put(ScmRestArgDefine.SITE_CONF_UPDATOR, updator);
        return obj;
    }
}
