package com.sequoiacm.config.framework.user.dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.config.framework.user.metasource.UserMetaService;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersion;
import com.sequoiacm.infrastructure.config.core.msg.DefaultVersionFilter;
import com.sequoiacm.infrastructure.config.core.msg.Version;

@Component
public class GetUserDao {

    @Autowired
    private UserMetaService userMetaService;

    public List<Version> getVersions(DefaultVersionFilter filter) throws ScmConfigException {
        List<Version> versions = new ArrayList<>();
        BSONObject matcher = new BasicBSONObject();
        BSONObject obj = userMetaService.getPrivVersionTableDao().queryOne(matcher, null, null);
        if (obj != null) {
            versions.add(new DefaultVersion(filter.getBussinessType(), filter.getBussinessType(),
                    (int) obj.get("version")));
        }
        return versions;
    }
}
