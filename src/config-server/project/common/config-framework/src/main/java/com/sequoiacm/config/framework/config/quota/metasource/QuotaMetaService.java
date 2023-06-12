package com.sequoiacm.config.framework.config.quota.metasource;

import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbTableDao;

public interface QuotaMetaService {
    SequoiadbTableDao getQuotaTable(Transaction transaction);

    SequoiadbTableDao getQuotaTable();
}
