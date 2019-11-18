package com.sequoiacm.config.metasource;

import com.sequoiacm.config.metasource.exception.MetasourceException;

public interface Transaction {
    public void begin() throws MetasourceException;

    public void commit() throws MetasourceException;

    public void rollback();

    public void close();
}
