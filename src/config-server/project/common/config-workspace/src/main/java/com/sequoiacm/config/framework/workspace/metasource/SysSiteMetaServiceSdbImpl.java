package com.sequoiacm.config.framework.workspace.metasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;

@Repository
public class SysSiteMetaServiceSdbImpl implements SysSiteMetaService {
    @Autowired
    private SequoiadbMetasource sdbMetasource;

    @Override
    public TableDao getSysSiteTable() {
        return sdbMetasource.getCollection(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_SITE);
    }
}
