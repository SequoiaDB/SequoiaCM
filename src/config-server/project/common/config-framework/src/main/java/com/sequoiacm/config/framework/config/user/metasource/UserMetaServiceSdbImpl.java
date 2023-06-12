package com.sequoiacm.config.framework.config.user.metasource;

import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserMetaServiceSdbImpl implements UserMetaService {

    @Autowired
    private SequoiadbMetasource sdbMetasource;

    @Override
    public TableDao getPrivVersionTableDao() {
        return sdbMetasource.getCollection(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_PRIV_VERSION);
    }
}
