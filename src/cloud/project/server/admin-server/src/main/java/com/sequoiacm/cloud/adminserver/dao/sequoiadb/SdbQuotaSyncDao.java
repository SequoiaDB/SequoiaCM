package com.sequoiacm.cloud.adminserver.dao.sequoiadb;

import com.sequoiacm.cloud.adminserver.common.SequoiadbHelper;
import com.sequoiacm.cloud.adminserver.dao.QuotaSyncDao;
import com.sequoiacm.cloud.adminserver.exception.ScmMetasourceException;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaAccessor;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiacm.cloud.adminserver.metasource.sequoiadb.SequoiadbMetaSource;
import com.sequoiacm.cloud.adminserver.model.QuotaSyncInfo;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmQuotaSyncStatus;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

@Repository
public class SdbQuotaSyncDao implements QuotaSyncDao {

    @Autowired
    private SequoiadbMetaSource metasource;

    @PostConstruct
    public void init() throws ScmMetasourceException {
        MetaAccessor accessor = metasource.getQuotaSyncAccessor();
        accessor.ensureTable();
        BasicBSONObject indexBson = new BasicBSONObject();
        indexBson.put(FieldName.QuotaSync.TYPE, 1);
        indexBson.put(FieldName.QuotaSync.NAME, 1);
        accessor.ensureIndex("idx_type_name", indexBson, true);
    }

    @Override
    public QuotaSyncInfo getQuotaSyncInfo(String type, String name, Integer syncRoundNumber)
            throws StatisticsException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.QuotaSync.TYPE, type);
        matcher.put(FieldName.QuotaSync.NAME, name);
        if (syncRoundNumber != null) {
            matcher.put(FieldName.QuotaSync.SYNC_ROUND_NUMBER, syncRoundNumber);
        }
        BSONObject res = SequoiadbHelper.queryOne(metasource.getQuotaSyncAccessor(), matcher, null);
        if (res == null) {
            return null;
        }
        return new QuotaSyncInfo(res);
    }

    @Override
    public void updateQuotaSyncInfo(QuotaSyncInfo quotaSyncInfo) throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.QuotaSync.TYPE, quotaSyncInfo.getType());
        matcher.put(FieldName.QuotaSync.NAME, quotaSyncInfo.getName());
        BSONObject record = new BasicBSONObject();
        record.put(FieldName.QuotaSync.TYPE, quotaSyncInfo.getType());
        record.put(FieldName.QuotaSync.NAME, quotaSyncInfo.getName());
        record.put(FieldName.QuotaSync.STATUS, quotaSyncInfo.getStatus());
        record.put(FieldName.QuotaSync.BEGIN_TIME, quotaSyncInfo.getBeginTime());
        record.put(FieldName.QuotaSync.END_TIME, quotaSyncInfo.getEndTime());
        record.put(FieldName.QuotaSync.SYNC_ROUND_NUMBER, quotaSyncInfo.getSyncRoundNumber());
        record.put(FieldName.QuotaSync.QUOTA_ROUND_NUMBER, quotaSyncInfo.getQuotaRoundNumber());
        record.put(FieldName.QuotaSync.AGREEMENT_TIME, quotaSyncInfo.getAgreementTime());
        record.put(FieldName.QuotaSync.STATISTICS_DETAIL, quotaSyncInfo.getStatisticsDetail());
        record.put(FieldName.QuotaSync.IS_FIRST_SYNC, quotaSyncInfo.isFirstSync());
        record.put(FieldName.QuotaSync.ERROR_MSG, quotaSyncInfo.getErrorMsg());
        record.put(FieldName.QuotaSync.EXTRA_INFO, quotaSyncInfo.getExtraInfo());
        record.put(FieldName.QuotaSync.UPDATE_TIME, System.currentTimeMillis());
        record.put(FieldName.QuotaSync.EXPIRE_TIME, quotaSyncInfo.getExpireTime());
        record.put(FieldName.QuotaSync.STATISTICS_SIZE, quotaSyncInfo.getStatisticsSize());
        record.put(FieldName.QuotaSync.STATISTICS_OBJECTS, quotaSyncInfo.getStatisticsObjects());
        metasource.getQuotaSyncAccessor().upsert(new BasicBSONObject("$set", record), matcher);
    }

    @Override
    public void updateAgreementTime(String type, String name, int syncRoundNumber,
            long agreementTime) throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.QuotaSync.TYPE, type);
        matcher.put(FieldName.QuotaSync.NAME, name);
        matcher.put(FieldName.QuotaSync.SYNC_ROUND_NUMBER, syncRoundNumber);
        BSONObject record = new BasicBSONObject();
        record.put(FieldName.QuotaSync.AGREEMENT_TIME, agreementTime);
        record.put(FieldName.QuotaSync.UPDATE_TIME, System.currentTimeMillis());
        metasource.getQuotaSyncAccessor().upsert(new BasicBSONObject("$set", record), matcher);
    }

    @Override
    public void recordError(String type, String name, int syncRoundNumber, String message)
            throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.QuotaSync.TYPE, type);
        matcher.put(FieldName.QuotaSync.NAME, name);
        matcher.put(FieldName.QuotaSync.SYNC_ROUND_NUMBER, syncRoundNumber);
        BSONObject record = new BasicBSONObject();
        record.put(FieldName.QuotaSync.ERROR_MSG, message);
        record.put(FieldName.QuotaSync.STATUS, ScmQuotaSyncStatus.FAILED.getName());
        record.put(FieldName.QuotaSync.UPDATE_TIME, System.currentTimeMillis());
        metasource.getQuotaSyncAccessor().upsert(new BasicBSONObject("$set", record), matcher);
    }

    @Override
    public void recordCompleted(String type, String name, int syncRoundNumber)
            throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.QuotaSync.TYPE, type);
        matcher.put(FieldName.QuotaSync.NAME, name);
        matcher.put(FieldName.QuotaSync.SYNC_ROUND_NUMBER, syncRoundNumber);
        BSONObject record = new BasicBSONObject();
        record.put(FieldName.QuotaSync.STATUS, ScmQuotaSyncStatus.COMPLETED.getName());
        record.put(FieldName.QuotaSync.END_TIME, System.currentTimeMillis());
        record.put(FieldName.QuotaSync.UPDATE_TIME, System.currentTimeMillis());
        metasource.getQuotaSyncAccessor().upsert(new BasicBSONObject("$set", record), matcher);
    }

    @Override
    public void updateSyncStatisticsDetail(String type, String name, int syncRoundNumber,
            BSONObject statisticsDetail) throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.QuotaSync.TYPE, type);
        matcher.put(FieldName.QuotaSync.NAME, name);
        matcher.put(FieldName.QuotaSync.SYNC_ROUND_NUMBER, syncRoundNumber);
        BSONObject record = new BasicBSONObject();
        record.put(FieldName.QuotaSync.STATISTICS_DETAIL, statisticsDetail);
        record.put(FieldName.QuotaSync.UPDATE_TIME, System.currentTimeMillis());
        metasource.getQuotaSyncAccessor().upsert(new BasicBSONObject("$set", record), matcher);
    }

    @Override
    public void cancelSync(String type, String name) throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.QuotaSync.TYPE, type);
        matcher.put(FieldName.QuotaSync.NAME, name);
        BSONObject record = new BasicBSONObject();
        record.put(FieldName.QuotaSync.STATUS, ScmQuotaSyncStatus.CANCELED.getName());
        record.put(FieldName.QuotaSync.UPDATE_TIME, System.currentTimeMillis());
        metasource.getQuotaSyncAccessor().upsert(new BasicBSONObject("$set", record), matcher);
    }

    @Override
    public void delete(BSONObject matcher) throws ScmMetasourceException {
        metasource.getQuotaSyncAccessor().delete(matcher);
    }

    @Override
    public long getSyncStatusCount(String type) throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.QuotaSync.TYPE, type);
        matcher.put(FieldName.QuotaSync.STATUS, ScmQuotaSyncStatus.SYNCING.getName());
        return metasource.getQuotaSyncAccessor().getCount(matcher);
    }

    @Override
    public MetaCursor querySyncStatusRecord() throws ScmMetasourceException {
        BSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.QuotaSync.STATUS, ScmQuotaSyncStatus.SYNCING.getName());
        return metasource.getQuotaSyncAccessor().query(matcher, null, null);
    }
}
