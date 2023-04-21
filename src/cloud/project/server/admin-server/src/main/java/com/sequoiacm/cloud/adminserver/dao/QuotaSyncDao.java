package com.sequoiacm.cloud.adminserver.dao;

import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiacm.cloud.adminserver.model.QuotaSyncInfo;
import org.bson.BSONObject;

public interface QuotaSyncDao {

    QuotaSyncInfo getQuotaSyncInfo(String type, String name, Integer syncRoundNumber)
            throws StatisticsException;

    void updateQuotaSyncInfo(QuotaSyncInfo quotaSyncInfo) throws ScmMetasourceException;

    void updateAgreementTime(String type, String name, int syncRoundNumber, long agreementTime)
            throws ScmMetasourceException;

    void recordError(String type, String name, int syncRoundNumber, String message)
            throws ScmMetasourceException;

    void recordCompleted(String type, String name, int syncRoundNumber)
            throws ScmMetasourceException;

    void updateSyncStatisticsDetail(String type, String name, int syncRoundNumber,
            BSONObject syncProgressDetail) throws ScmMetasourceException;

    void cancelSync(String type, String name) throws ScmMetasourceException;

    void delete(BSONObject matcher) throws ScmMetasourceException;

    long getSyncStatusCount(String type) throws ScmMetasourceException;

    MetaCursor querySyncStatusRecord() throws ScmMetasourceException;
}
