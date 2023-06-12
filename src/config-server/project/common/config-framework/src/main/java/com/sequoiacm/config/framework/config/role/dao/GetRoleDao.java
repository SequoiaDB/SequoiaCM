package com.sequoiacm.config.framework.config.role.dao;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.config.framework.config.role.metasource.RoleMetaService;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.Version;
import com.sequoiacm.infrastructure.config.core.msg.VersionFilter;

@Component
public class GetRoleDao {

    @Autowired
    private RoleMetaService roleMetaService;

    public List<Version> getVersions(VersionFilter filter) throws ScmConfigException {
        List<Version> versions = new ArrayList<>();
        BSONObject matcher = new BasicBSONObject();
        BSONObject obj = roleMetaService.getPrivVersionTableDao().queryOne(matcher, null, null);
        if (obj != null) {
            versions.add(new Version(filter.getBusinessType(), filter.getBusinessType(),
                    (int) obj.get("version")));
        }
        return versions;
    }
}
