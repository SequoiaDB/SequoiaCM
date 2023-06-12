package com.sequoiacm.config.framework.config.node.metasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;

@Repository
public class NodeMetaServiceImpl implements NodeMetaService {
    @Autowired
    private SequoiadbMetasource sdbMetasource;

    @Override
    public TableDao getContentServerTableDao(Transaction transaction) {
        return sdbMetasource.getCollection(transaction,
                MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_CONTENTSERVER);
    }

    @Override
    public TableDao getContentServerTableDao() {
        return sdbMetasource.getCollection(MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_CONTENTSERVER);
    }

}
