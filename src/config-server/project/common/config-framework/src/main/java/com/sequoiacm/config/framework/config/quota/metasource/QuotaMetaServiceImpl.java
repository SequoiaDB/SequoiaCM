package com.sequoiacm.config.framework.config.quota.metasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbTableDao;

@Repository
public class QuotaMetaServiceImpl implements QuotaMetaService {

    @Autowired
    private SequoiadbMetasource sdbMetasource;

    @Override
    public SequoiadbTableDao getQuotaTable(Transaction transaction) {
        return sdbMetasource.getCollection(transaction,
                MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_QUOTA);
    }

    @Override
    public SequoiadbTableDao getQuotaTable() {
        return sdbMetasource.getCollection(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_QUOTA);
    }
}
