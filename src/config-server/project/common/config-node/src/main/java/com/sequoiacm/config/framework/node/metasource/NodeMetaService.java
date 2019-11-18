package com.sequoiacm.config.framework.node.metasource;

import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;

public interface NodeMetaService {
    TableDao getContentServerTableDao(Transaction transaction);

    TableDao getContentServerTableDao();

}
