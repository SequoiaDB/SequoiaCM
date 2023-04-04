package com.sequoiacm.infrastructure.config.core.msg.role;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.infrastructure.config.core.common.ScmRestArgDefine;
import com.sequoiacm.infrastructure.config.core.msg.ConfigFilter;

public class RoleFilter implements ConfigFilter {

    private String name;

    public RoleFilter(String name) {
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
        BasicBSONObject obj = new BasicBSONObject();
        if (name != null) {
            obj.put(ScmRestArgDefine.ROLE_CONF_ROLENAME, name);
        }
        return obj;
    }
}
