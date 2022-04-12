package com.sequoiacm.contentserver.contentmodule;

import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;

public interface TransactionCallback {
    void beforeTransactionCommit(TransactionContext context)
            throws ScmServerException, ScmMetasourceException;
}
