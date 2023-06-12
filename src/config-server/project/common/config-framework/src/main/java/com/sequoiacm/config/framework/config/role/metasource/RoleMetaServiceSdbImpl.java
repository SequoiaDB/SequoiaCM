package com.sequoiacm.config.framework.config.role.metasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;

@Repository
public class RoleMetaServiceSdbImpl implements RoleMetaService {

    @Autowired
    private SequoiadbMetasource sdbMetasource;

    @Override
    public TableDao getPrivVersionTableDao() {
        return sdbMetasource.getCollection(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_PRIV_VERSION);
    }
}
