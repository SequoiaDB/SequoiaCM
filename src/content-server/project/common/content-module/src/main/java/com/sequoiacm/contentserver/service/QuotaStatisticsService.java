package com.sequoiacm.contentserver.service;

import com.sequoiacm.exception.ScmServerException;

public interface QuotaStatisticsService {
    void doStatistics(String type, String name, int syncRoundNumber) throws ScmServerException;
}
