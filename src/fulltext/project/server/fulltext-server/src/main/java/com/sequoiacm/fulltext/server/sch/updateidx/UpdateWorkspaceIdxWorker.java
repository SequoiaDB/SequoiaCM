package com.sequoiacm.fulltext.server.sch.updateidx;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.content.client.ContentserverClient;
import com.sequoiacm.content.client.ContentserverClientMgr;
import com.sequoiacm.content.client.ScmEleCursor;
import com.sequoiacm.content.client.model.ScmFileInfo;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.fulltext.es.client.base.EsClient;
import com.sequoiacm.fulltext.server.ConfServiceClient;
import com.sequoiacm.fulltext.server.WsFulltextExtDataModifier;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.fileidx.FileIdxDao;
import com.sequoiacm.fulltext.server.fileidx.FileIdxDaoFactory;
import com.sequoiacm.fulltext.server.lock.LockManager;
import com.sequoiacm.fulltext.server.lock.LockPathFactory;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobData;
import com.sequoiacm.fulltext.server.sch.IdxThreadPool;
import com.sequoiacm.fulltext.server.sch.createidx.CreateWorkspaceIdxWorker;
import com.sequoiacm.fulltext.server.sch.createidx.CreateWorkspaceIdxWorkerConfig;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOperation;
import com.sequoiacm.infrastructure.fulltext.common.FulltextCommonDefine;
import com.sequoiacm.infrastructure.fulltext.common.ScmFileFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.client.core.ProducerClient;
import com.sequoiacm.schedule.common.model.ScheduleException;

public class UpdateWorkspaceIdxWorker extends CreateWorkspaceIdxWorker {

    private static final Logger logger = LoggerFactory.getLogger(UpdateWorkspaceIdxWorker.class);
    private final FileIdxDaoFactory scmFileIdDaoFactory;
    private AdminClient mqAdmin;

    public UpdateWorkspaceIdxWorker(FileIdxDaoFactory scmFileIdDaoFactory, CreateWorkspaceIdxWorkerConfig conf, EsClient esClient,
            ContentserverClientMgr csMgr, ScmSiteInfoMgr siteInfoMgr, ConfServiceClient confClient,
            LockManager lockMgr, LockPathFactory lockPathFactory, AdminClient adminClient,
            IdxThreadPool idxThreadPool, ProducerClient producerClient) {
        super(conf, esClient, csMgr, siteInfoMgr, confClient, lockMgr, lockPathFactory,
                idxThreadPool, producerClient, adminClient);
        this.mqAdmin = adminClient;
        this.scmFileIdDaoFactory = scmFileIdDaoFactory;
    }

    @Override
    protected void exec(String schName, BSONObject jobData) throws Exception {
        FulltextIdxSchJobData data = new FulltextIdxSchJobData(jobData);
        boolean isConsumed = mqAdmin.waitForMsgConsumed(FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC,
                FulltextCommonDefine.FULLTEXT_GROUP_NAME, data.getLatestMsgId(), true,
                Integer.MAX_VALUE, 5000);
        if (!isConsumed) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to wait msg to be consumed, timeout: topic="
                            + FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC + ", msgId="
                            + data.getLatestMsgId());
        }

        String rootSite = siteInfoMgr.getRootSiteName();
        ContentserverClient csClient = csMgr.getClient(rootSite);

        // 返回 null 表示没有文件需要删除索引
        BSONObject dropIdxCondition = conditionForDropIndex(data);

        BSONObject createIdxCondition = conditionForCreateIndex(data);

        long estimateCount = getEstimateFileCount(data, csClient, dropIdxCondition,
                createIdxCondition);
        getStatus().setEstimateCount(estimateCount);
        reportInitStatus();

        dropIndex(data, csClient, dropIdxCondition);
        // 等删除索引的子任务全部完成后，再开始建索引，避免可能存在建索引和删除索引同时作用在一个文件上
        waitSubTaskExit();

        createIndex(data, csClient, createIdxCondition);
        waitSubTaskExit();

        esClient.refreshIndexSilence(data.getIndexDataLocation());

        WsFulltextExtDataModifier modifier = new WsFulltextExtDataModifier(data.getWs(), schName);
        modifier.setIndexStatus(ScmFulltextStatus.CREATED);
        ScmLock lock = acquiresFulltextLockAndCheckStopFlag(data.getWs());
        if (lock == null) {
            // 没拿到锁说明任务已经被停了
            return;
        }
        try {
            confClient.updateWsExternalData(modifier);
        }
        finally {
            lock.unlock();
        }

        reportStatus(true);
    }

    private void dropIndex(FulltextIdxSchJobData data, ContentserverClient csClient,
            BSONObject dropIdxCondition)
            throws ScmServerException, ScheduleException, FullTextException {
        if (dropIdxCondition == null) {
            logger.info("fulltext matcher is empty, no need drop index:ws={}, matcher={}",
                    data.getWs(), data.getFileMatcher());
            return;
        }
        ScmEleCursor<ScmFileInfo> cursor = csClient.listFile(data.getWs(), dropIdxCondition,
                CommonDefine.Scope.SCOPE_CURRENT, null, 0, -1);
        try {
            while (cursor.hasNext()) {
                if (isStop()) {
                    logger.info("worker catch stop signal, worker is stoping:schId={}, name={}",
                            getScheduleId(), getScheduleName());
                    break;
                }
                ScmFileInfo file = cursor.getNext();
                FileFulltextOperation op = new FileFulltextOperation();
                op.setOperationType(FileFulltextOperation.OperationType.DROP_IDX_AND_UPDATE_FILE);
                op.setWsName(data.getWs());
                op.setIndexLocation(data.getIndexDataLocation());
                op.setFileId(file.getId());

                FileIdxDao dao = scmFileIdDaoFactory.createDao(op);
                DropAndUpdateFileIdxTask task = new DropAndUpdateFileIdxTask(dao, getTaskContext());
                submit(task);
                getTaskContext().incTaskCount();
                reportStatus(false);
            }
        }
        finally {
            cursor.close();
        }
    }

    private long getEstimateFileCount(FulltextIdxSchJobData data, ContentserverClient csClient,
            BSONObject dropIdxCondition, BSONObject createIdxCondition) throws ScmServerException {
        BasicBSONList orList = new BasicBSONList();
        orList.add(createIdxCondition);
        if (dropIdxCondition != null) {
            orList.add(dropIdxCondition);
        }
        BasicBSONObject and = new BasicBSONObject("$or", orList);
        return csClient.countFile(data.getWs(), CommonDefine.Scope.SCOPE_CURRENT, and);
    }

    private BSONObject conditionForDropIndex(FulltextIdxSchJobData data) {
        /*
         * { "$and": [{ "external_data.fulltext_status": { "$ne": "NONE" } }, { "$not":
         * [{ "title": "fileTitle" }] }] }
         */
        // 工作区的索引条件是空，表示所有文件都需要建索引，那么本次更新索引没有文件需要删除索引，返回一个空指针表示这种情况
        if (data.getFileMatcher() == null || data.getFileMatcher().isEmpty()) {
            return null;
        }

        BasicBSONObject hasIndexFile = new BasicBSONObject();
        BasicBSONObject notEq = new BasicBSONObject("$ne", ScmFileFulltextStatus.NONE.name());
        hasIndexFile.put(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + "."
                + ScmFileFulltextExtData.FIELD_IDX_STATUS, notEq);

        BasicBSONObject noNeedIndexFile = new BasicBSONObject();
        BasicBSONList notList = new BasicBSONList();
        notList.add(data.getFileMatcher());
        noNeedIndexFile.put("$not", notList);

        BasicBSONList andList = new BasicBSONList();
        andList.add(hasIndexFile);
        andList.add(noNeedIndexFile);
        return new BasicBSONObject("$and", andList);
    }

    private BSONObject conditionForCreateIndex(FulltextIdxSchJobData data) {
        BasicBSONObject noIndexFile = new BasicBSONObject();

        BasicBSONObject notEq = new BasicBSONObject("$ne", ScmFileFulltextStatus.CREATED.name());
        noIndexFile.put(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + "."
                + ScmFileFulltextExtData.FIELD_IDX_STATUS, notEq);

        BasicBSONList andList = new BasicBSONList();
        andList.add(noIndexFile);
        andList.add(data.getFileMatcher());
        return new BasicBSONObject("$and", andList);
    }

}
