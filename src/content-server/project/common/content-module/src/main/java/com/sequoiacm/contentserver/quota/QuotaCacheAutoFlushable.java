package com.sequoiacm.contentserver.quota;

public interface QuotaCacheAutoFlushable {

    void flushCache(boolean force) throws QuotaLimiterIncorrectException;
}
