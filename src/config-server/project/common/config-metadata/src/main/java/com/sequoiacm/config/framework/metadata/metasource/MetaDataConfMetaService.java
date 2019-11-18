package com.sequoiacm.config.framework.metadata.metasource;

import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;

public interface MetaDataConfMetaService {
    public TableDao getAttributeTableDao(String wsName, Transaction t) throws MetasourceException;

    public TableDao getClassTableDao(String wsName, Transaction t) throws MetasourceException;

    public TableDao getAttributeClassRelTableDao(String wsName, Transaction t)
            throws MetasourceException;
}
