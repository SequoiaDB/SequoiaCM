package com.sequoiacm.config.metasource;

import com.sequoiacm.config.metasource.exception.MetasourceException;

public interface Metasource {

    public TableDao getSubscribersTable() throws MetasourceException;

    public TableDao getConfVersionTableDao() throws MetasourceException;

    public TableDao getConfVersionTableDao(Transaction transaction) throws MetasourceException;

    public Transaction createTransaction() throws MetasourceException;

    public ScmGlobalConfigTableDao getScmGlobalConfigTableDao() throws MetasourceException;

    public MetasourceType getType();
}
