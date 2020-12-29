package com.sequoiacm.fulltext.server.operator;

import java.util.UUID;

import com.sequoiacm.fulltext.server.config.FulltextMqConfig;
import com.sequoiacm.mq.core.exception.MqError;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.ConfServiceClient;
import com.sequoiacm.fulltext.server.WsFulltextExtDataModifier;
import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobData;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobType;
import com.sequoiacm.fulltext.server.sch.ScheduleServiceClient;
import com.sequoiacm.infrastructure.fulltext.common.FulltextCommonDefine;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.core.exception.MqException;

public abstract class FulltextIdxOperator {
    private static final Logger logger = LoggerFactory.getLogger(FulltextIdxOperator.class);

    @Autowired
    protected ConfServiceClient confClient;
    @Autowired
    protected ScheduleServiceClient schClient;
    @Autowired
    protected EsClient esClient;
    @Autowired
    protected AdminClient mqAdminClient;
    @Autowired
    private FulltextMqConfig mqConfig;

    @Autowired
    public FulltextIdxOperator() {
    }

    public abstract void createIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData,
            BSONObject fileMatcher, ScmFulltextMode mode) throws FullTextException;

    public abstract void dropIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData)
            throws FullTextException;

    public abstract void updateIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData,
            BSONObject newFileMatcher, ScmFulltextMode newMode) throws FullTextException;

    public abstract ScmFulltextStatus operatorForStatus();

    protected void createTopicIfNotExist(String wsName) throws FullTextException {
        try {
            mqAdminClient.createTopicIfNotExist(FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC,
                    mqConfig.getTopicPartitionNum());
        }
        catch (MqException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to create topic for in mq-server:"
                            + FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC,
                    e);
        }
    }

    public abstract void rebuildIndex(ScmWorkspaceFulltextExtData currentWsFulltextExtData,
            String fileId) throws FullTextException;

    protected long getLatestMsgId(String ws) throws FullTextException {
        try {
            return mqAdminClient.peekLatestMsgId(FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC);
        }
        catch (MqException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to get latest msg from mq-server", e);
        }

    }

    protected void dropIndexSilence(String indexName) {
        try {
            esClient.dropIndex(indexName);
        }
        catch (FullTextException e) {
            logger.warn("rollback failed, drop index failed:indexName={}", indexName, e);
        }
    }

    protected void removeSchSilence(String schJobName) {
        try {
            schClient.removeInternalSch(schJobName, false);
        }
        catch (Exception e) {
            logger.warn("rollback failed, remove schedule failed:scheduleName=" + schJobName, e);
        }
    }

    public abstract void inspectIndex(ScmWorkspaceFulltextExtData fulltextData)
            throws FullTextException;

    protected void changeToCreateing(String wsName, BSONObject fileMatcher, ScmFulltextMode mode,
            String indexLocation, String schName) throws FullTextException {
        WsFulltextExtDataModifier modifier = new WsFulltextExtDataModifier(wsName);
        modifier.setEnabled(true);
        modifier.setFileMatcher(fileMatcher);
        modifier.setIndexStatus(ScmFulltextStatus.CREATING);
        modifier.setMode(mode);
        modifier.setFulltextJobName(schName);
        modifier.setIndexDataLocation(indexLocation);
        confClient.updateWsExternalData(modifier);
    }

    protected void createSchForIndexCreate(String wsName, BSONObject fileMatcher,
            String indexLocation, String schName) throws FullTextException {
        FulltextIdxSchJobData fulltextSch = new FulltextIdxSchJobData();
        fulltextSch.setFileMatcher(fileMatcher);
        fulltextSch.setIndexDataLocation(indexLocation);
        fulltextSch.setWs(wsName);
        schClient.createFulltextSch(schName, FulltextIdxSchJobType.FULLTEXT_INDEX_CREATE,
                fulltextSch);
    }

    protected void changeToDeletingAndCreateSch(String wsName, String idxLocation)
            throws FullTextException {
        String schName = FulltextCommonDefine.FULLTEXT_SCHEDULE_PREFIX + wsName + "-"
                + FulltextIdxSchJobType.FULLTEXT_INDEX_DELETE + "-" + UUID.randomUUID().toString();

        WsFulltextExtDataModifier modifier = new WsFulltextExtDataModifier(wsName);
        modifier.setEnabled(false);
        modifier.setFileMatcher(null);
        modifier.setIndexStatus(ScmFulltextStatus.DELETING);
        modifier.setMode(null);
        modifier.setFulltextJobName(schName);
        confClient.updateWsExternalData(modifier);

        long latestMsgId = getLatestMsgId(wsName);

        FulltextIdxSchJobData fulltextSch = new FulltextIdxSchJobData();
        fulltextSch.setFileMatcher(null);
        fulltextSch.setIndexDataLocation(idxLocation);
        fulltextSch.setWs(wsName);
        fulltextSch.setLatestMsgId(latestMsgId);
        schClient.createFulltextSch(schName, FulltextIdxSchJobType.FULLTEXT_INDEX_DELETE,
                fulltextSch);
    }

    protected void rollbackToNoneSilence(String wsName) {
        WsFulltextExtDataModifier modifier = new WsFulltextExtDataModifier(wsName);
        modifier.setEnabled(false);
        modifier.setFileMatcher(null);
        modifier.setFulltextJobName(null);
        modifier.setIndexDataLocation(null);
        modifier.setIndexStatus(ScmFulltextStatus.NONE);
        modifier.setMode(null);
        try {
            confClient.updateWsExternalData(modifier);
        }
        catch (Exception e) {
            logger.warn("rollback failed, failed to rollback workspace fulltext external data:ws="
                    + wsName, e);
        }
    }
}
