package com.sequoiacm.config.framework.config.site.metasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;

@Repository
public class SiteMetaServiceSdbImpl implements SiteMetaService {
    @Autowired
    private SequoiadbMetasource sdbMetasource;

    @Override
    public TableDao getSysSiteTable(Transaction transaction) {
        if (transaction == null) {
            return sdbMetasource.getCollection(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                    MetaSourceDefine.SequoiadbTableName.CL_SITE);
        }
        return sdbMetasource.getCollection(transaction,
                MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_SITE);
    }

    @Override
    public TableDao getSysSiteTable() {
        return sdbMetasource.getCollection(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_SITE);
    }
}
