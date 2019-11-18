package com.sequoiacm.cloud.authentication.dao;

import com.sequoiacm.infrastructrue.security.core.ITransaction;

public interface IPrivVersionDao {
    int getVersion();

    void incVersion();

    void incVersion(ITransaction t);
}
