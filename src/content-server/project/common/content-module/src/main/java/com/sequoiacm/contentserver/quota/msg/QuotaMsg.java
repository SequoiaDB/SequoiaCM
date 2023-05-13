package com.sequoiacm.contentserver.quota.msg;

public interface QuotaMsg {

    String getType();

    String getName();

    int getQuotaRoundNumber();
}
