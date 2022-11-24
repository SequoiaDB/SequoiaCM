package com.sequoiacm.s3.transactioncallback;

import com.sequoiacm.contentserver.contentmodule.TransactionCallback;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.metasource.ScmMetasourceException;
import com.sequoiacm.metasource.TransactionContext;
import com.sequoiacm.s3.core.UploadMeta;
import com.sequoiacm.s3.dao.UploadDao;

public class TransactionCallbackUpload implements TransactionCallback {

    private UploadMeta uploadMeta;

    private UploadDao uploadDao;

    public TransactionCallbackUpload(UploadMeta uploadMeta, UploadDao uploadDao) {
        this.uploadMeta = uploadMeta;
        this.uploadDao = uploadDao;
    }

    @Override
    public void beforeTransactionCommit(TransactionContext context)
            throws ScmServerException, ScmMetasourceException {
        try {
            uploadDao.updateUploadMeta(context, uploadMeta);
        }
        catch (Exception e) {
            throw new ScmMetasourceException(
                    "update upload meta failed. uploadMeta: " + uploadMeta.toString(), e);
        }
    }
}
