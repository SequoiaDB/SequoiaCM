package com.sequoiacm.contentserver.quota;

import com.sequoiacm.contentserver.quota.limiter.QuotaLimiter;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;

public class QuotaLimiterIncorrectException extends ScmServerException {

    private QuotaLimiter currentQuotaLimiter;

    public QuotaLimiterIncorrectException(QuotaLimiter currentQuotaLimiter) {
        super(ScmError.SYSTEM_ERROR, "quota limiter incorrect:bucketName="
                + currentQuotaLimiter.getBucketName() + ", type=" + currentQuotaLimiter.getType());
        this.currentQuotaLimiter = currentQuotaLimiter;
    }

    public QuotaLimiter getCurrentQuotaLimiter() {
        return currentQuotaLimiter;
    }
}
