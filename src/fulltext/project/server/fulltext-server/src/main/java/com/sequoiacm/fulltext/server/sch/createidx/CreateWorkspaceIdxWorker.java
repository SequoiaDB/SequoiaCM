package com.sequoiacm.fulltext.server.sch.createidx;

import java.util.ArrayList;
import java.util.List;

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
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.fulltext.es.client.base.EsClient;
import com.sequoiacm.fulltext.server.ConfServiceClient;
import com.sequoiacm.fulltext.server.WsFulltextExtDataModifier;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.lock.LockManager;
import com.sequoiacm.fulltext.server.lock.LockPathFactory;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobData;
import com.sequoiacm.fulltext.server.sch.IdxThreadPool;
import com.sequoiacm.fulltext.server.sch.WorkspaceIdxWorkerBase;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOperation;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOperations;
import com.sequoiacm.infrastructure.fulltext.common.FulltextCommonDefine;
import com.sequoiacm.infrastructure.fulltext.common.ScmFileFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.client.core.ProducerClient;
import com.sequoiacm.mq.client.core.SerializableMessage;
import com.sequoiacm.mq.core.exception.MqException;

public class CreateWorkspaceIdxWorker extends WorkspaceIdxWorkerBase {
    private static final Logger logger = LoggerFactory.getLogger(CreateWorkspaceIdxWorker.class);
    private final int fileCountInOnMsg;
    private final int msgCountToCheckConsume;
    protected ContentserverClientMgr csMgr;
    protected ScmSiteInfoMgr siteInfoMgr;
    protected EsClient esClient;
    protected ConfServiceClient confClient;
    private FulltextIdxSchJobData jobData;

    protected LockManager lockMgr;
    protected LockPathFactory lockPathFactory;
    protected ProducerClient producerClient;
    protected AdminClient adminClient;

    public CreateWorkspaceIdxWorker(CreateWorkspaceIdxWorkerConfig conf, EsClient esClient,
            ContentserverClientMgr csMgr, ScmSiteInfoMgr siteInfoMgr, ConfServiceClient confClient,
            LockManager lockMgr, LockPathFactory lockPathFactory, IdxThreadPool idxThreadPool,
            ProducerClient producerClient, AdminClient adminClient) {
        super(idxThreadPool);
        this.esClient = esClient;
        this.csMgr = csMgr;
        this.siteInfoMgr = siteInfoMgr;
        this.confClient = confClient;
        this.lockMgr = lockMgr;
        this.lockPathFactory = lockPathFactory;
        this.producerClient = producerClient;
        this.adminClient = adminClient;
        this.msgCountToCheckConsume = conf.getMsgCountToCheckConsumed();
        this.fileCountInOnMsg = conf.getFilesInOneMsg();
    }

    protected void createIndex(FulltextIdxSchJobData data, ContentserverClient csClient,
            BSONObject wsFulltextFileMatcher)
            throws ScmServerException, MqException, InterruptedException {
        // 工作区条件 && (不存在external_data字段 || external_data.status!=created)，
        BasicBSONObject notExistExternal = new BasicBSONObject(
                FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA, new BasicBSONObject("$exists", 0));
        BasicBSONObject existExternal = new BasicBSONObject(
                FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + "."
                        + ScmFileFulltextExtData.FIELD_IDX_STATUS,
                new BasicBSONObject("$ne", ScmFileFulltextStatus.CREATED.name()));
        BasicBSONList orCondList = new BasicBSONList();
        orCondList.add(notExistExternal);
        orCondList.add(existExternal);
        BasicBSONObject notCreatedMatcher = new BasicBSONObject("$or", orCondList);

        BasicBSONList andCondList = new BasicBSONList();
        andCondList.add(wsFulltextFileMatcher);
        andCondList.add(notCreatedMatcher);
        BasicBSONObject condition = new BasicBSONObject("$and", andCondList);

        long lastMsgId = -1;
        long waitMsgId = -1;
        List<ScmFileInfo> filesInOneMsg = new ArrayList<>();
        long sendMsgCount = 0;

        /*
          SEQUOIACM-561

            投递消息的count： 1  2 3  4... 5000 5001 5002 5003... 10000...
            投递消息的ID：    2  9 10 11.. 6021 6023 6024 6028... 11012...
                                          |                      |
                                        等待 waitMsgId 被消费      等待 waitMsgId 被消费
                                        重新赋值 waitMsgId=6021    重新赋值 waitMsgId=11012

          waitMsgId 初始值为 -1， 每投递 msgCountToCheckConsume（5000） 个消息，
          等待 waitMsgId 被消费，然后重新赋值 waitMsgId
         */
        try (ScmEleCursor<ScmFileInfo> cursor = csClient.listFile(data.getWs(), condition,
                CommonDefine.Scope.SCOPE_CURRENT, null, 0, -1)) {
            while (cursor.hasNext()) {
                if (isStop()) {
                    logger.info("worker catch stop signal, worker is stopping:schId={}, name={}",
                            getScheduleId(), getScheduleName());
                    return;
                }
                ScmFileInfo file = cursor.getNext();
                filesInOneMsg.add(file);
                if (filesInOneMsg.size() < fileCountInOnMsg) {
                    continue;
                }
                if (sendMsgCount < msgCountToCheckConsume) {
                    lastMsgId = sendMsg(data, filesInOneMsg);
                    filesInOneMsg.clear();
                    sendMsgCount++;
                    continue;
                }

                // 返回 false 如果等待的过程收到了停止信号
                if (!waitMsgBeConsumed(waitMsgId)) {
                    return;
                }

                waitMsgId = sendMsg(data, filesInOneMsg);
                filesInOneMsg.clear();
                lastMsgId = waitMsgId;
                sendMsgCount = 1;
            }
        }
        if (filesInOneMsg.size() > 0) {
            lastMsgId = sendMsg(data, filesInOneMsg);
        }
        waitMsgBeConsumed(lastMsgId);

        // latestMsgId 已经被消费，所有监听小于该ID的回调都做过期处理
        // 但由于消息队列的消息反馈是异步的，即某条消息被确认消费后，反馈可能还未到达生成者，
        // 所以对这些回调过期处理前，生产者额外再至多等 60s ，确保上述反馈可以正常收取到
        producerClient.triggerCallbackTimeout(FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC,
                FulltextCommonDefine.FULLTEXT_GROUP_NAME, lastMsgId, 60000);
    }

    private long sendMsg(FulltextIdxSchJobData data, List<ScmFileInfo> filesInOneMsg)
            throws MqException, InterruptedException {
        if (filesInOneMsg.size() <= 0) {
            return -1;
        }

        CreateWorkspaceIdxFeedbackCallback idxFeedbackCallback = new CreateWorkspaceIdxFeedbackCallback(this,
                filesInOneMsg.size());
        final FileFulltextOperations fs = new FileFulltextOperations();
        for (ScmFileInfo file : filesInOneMsg) {
            FileFulltextOperation fileOp = new FileFulltextOperation();
            fileOp.setFileId(file.getId());
            fileOp.setIndexLocation(data.getIndexDataLocation());
            fileOp.setOperationType(FileFulltextOperation.OperationType.CREATE_IDX);
            fileOp.setSyncSaveIndex(false);
            fileOp.setReindex(false);
            fileOp.setWsName(data.getWs());
            fs.add(fileOp);
        }
        return producerClient.putMsg(FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC,
                data.getWs() + "-" + filesInOneMsg.get(0).getId(), new SerializableMessage() {
                    @Override
                    public BSONObject serialize() {
                        return fs.toBSON();
                    }
                }, idxFeedbackCallback);
    }

    private boolean waitMsgBeConsumed(long waitMsgId) throws MqException, InterruptedException {
        while (true) {
            if (isStop()) {
                logger.info(
                        "worker catch stop signal, worker is stopping, give up on wait msg be consumed: topic={}, msgId={}",
                        FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC, waitMsgId);
                return false;
            }
            if (adminClient.waitForMsgConsumed(FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC,
                    FulltextCommonDefine.FULLTEXT_GROUP_NAME, waitMsgId, true, 5000, 1000)) {
                return true;
            }
        }
    }

    @Override
    protected void exec(String schName, BSONObject jobDataBson) throws Exception {
        jobData = new FulltextIdxSchJobData(jobDataBson);
        ContentserverClient csClient = csMgr.getClient(siteInfoMgr.getRootSiteName());

        long estimateCount = csClient.countFile(jobData.getWs(), CommonDefine.Scope.SCOPE_CURRENT,
                jobData.getFileMatcher());
        getStatus().setEstimateCount(estimateCount);
        reportInitStatus();

        createIndex(jobData, csClient, jobData.getFileMatcher());

        if (isStop()) {
            return;
        }

        esClient.refreshIndexSilence(jobData.getIndexDataLocation());

        WsFulltextExtDataModifier modifier = new WsFulltextExtDataModifier(jobData.getWs(),
                schName);
        modifier.setIndexStatus(ScmFulltextStatus.CREATED);

        ScmLock lock = acquiresFulltextLockAndCheckStopFlag(jobData.getWs());
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

    protected ScmLock acquiresFulltextLockAndCheckStopFlag(String ws) throws FullTextException {
        while (true) {
            if (isStop()) {
                return null;
            }
            ScmLock lock = lockMgr.acquiresLock(lockPathFactory.fulltextLockPath(ws), 2000);
            if (lock != null) {
                return lock;
            }
        }
    }

    protected void waitSubTaskExit() throws Exception {
        while (!getTaskContext().waitAllTaskFinish(5000)) {
            logger.info("wait subtask exit:schId={}, schName={}", getScheduleId(),
                    getScheduleName());
        }
    }

    @Override
    public String toString() {
        return "index create, jobData=" + jobData;
    }

}
