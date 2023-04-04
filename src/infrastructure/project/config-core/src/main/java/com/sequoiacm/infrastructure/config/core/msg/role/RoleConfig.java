package com.sequoiacm.infrastructure.config.core.msg.role;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.msg.Config;

public class RoleConfig implements Config {

    private String name;

    public RoleConfig() {

    }

    public RoleConfig(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String username) {
        this.name = name;
    }

    @Override
    public BSONObject toBSONObject() {
        BSONObject userConfigObj = new BasicBSONObject();
        userConfigObj.put(FieldName.Role.FIELD_ROLE_NAME, name);
        return userConfigObj;
    }


}
