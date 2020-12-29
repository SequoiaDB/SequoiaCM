package com.sequoiacm.fulltext.server.consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.fulltext.server.config.FulltextMqConfig;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.sch.IdxTaskContext;
import com.sequoiacm.fulltext.server.workspace.ScmWorkspaceMgr;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.fulltext.common.*;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOperation.OperationType;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;
import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.client.core.ConsumerClient;
import com.sequoiacm.mq.client.core.ConsumerClientMgr;
import com.sequoiacm.mq.client.core.FeedbackSerializer;
import com.sequoiacm.mq.client.core.MessageDeserializer;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroupOffsetEnum;
import com.sequoiacm.mq.core.module.Message;

public class IdxConsumerMainTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(IdxConsumerMainTask.class);
    private final IdxConsumerConfig conf;
    private boolean isClosed = false;

    private static final FeedbackSerializer<FileFulltextOpFeedback> feedbackSerializer = new FeedbackSerializer<FileFulltextOpFeedback>() {
        @Override
        public BSONObject serialize(FileFulltextOpFeedback fileFulltextOpFeedback) {
            BasicBSONObject ret = new BasicBSONObject();
            ret.put(FileFulltextOpFeedback.KEY_FAILED_COUNT,
                    fileFulltextOpFeedback.getFailedCount());
            ret.put(FileFulltextOpFeedback.KEY_SUCCESS_COUNT,
                    fileFulltextOpFeedback.getSuccessCount());
            return ret;
        }
    };

    private final FulltextMqConfig fulltextMqConfig;
    private IdxConsumerExecutor consumerExecutor;
    private AdminClient mqAdminClient;
    private ConsumerClientMgr mqConsumerClientMgr;
    private ConsumerClient<FileFulltextOperations, FileFulltextOpFeedback> client;
    private String topic;
    private final MessageDeserializer<FileFulltextOperations> msgDeserializer;

    public IdxConsumerMainTask(IdxConsumerConfig conf, AdminClient mqAdminClient,
            ConsumerClientMgr consumerClientMgr, IdxConsumerExecutor consumerExecutor,
            ScmWorkspaceMgr wsInfoMgr, FulltextMqConfig fulltextMqConfig) {
        this.mqAdminClient = mqAdminClient;
        this.mqConsumerClientMgr = consumerClientMgr;
        this.consumerExecutor = consumerExecutor;
        this.topic = FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC;
        this.fulltextMqConfig = fulltextMqConfig;
        this.conf = conf;
        this.msgDeserializer = new FileFulltextOperationsDeserializer(wsInfoMgr);
    }

    @Override
    public void run() {
        while (true) {
            try {
                pullAndConsume();
                if (isClosed) {
                    logger.info("consumer main task exit cause by close");
                    return;
                }
            }
            catch (Throwable e) {
                logger.error("consumer main task occur unexpected exception", e);
                if (isClosed) {
                    logger.info("consumer main task exit cause by close");
                    return;
                }
                try {
                    Thread.sleep(2000);
                }
                catch (Throwable ex) {
                    logger.error("consumer main task occur unexpected exception", ex);
                }
            }
        }
    }

    private void pullAndConsume() throws FullTextException, MqException, InterruptedException {
        if (client == null) {
            recreateClient();
        }

        List<Message<FileFulltextOperations>> msgs;
        try {
            msgs = client.pullMsg(conf.getPullMaxMsgAtOneTime(), 5000);
        }
        catch (MqException e) {
            logger.warn("failed to pull msg:topic=" + topic, e);
            recreateClient();
            return;
        }

        if (msgs == null) {
            return;
        }

        Map<String, List<Message<FileFulltextOperations>>> wsAndFileId2Msgs = new HashMap<>(
                conf.getPullMaxMsgAtOneTime());
        for (Message<FileFulltextOperations> m : msgs) {
            String wsAndFileId = m.getKey();
            List<Message<FileFulltextOperations>> sameFileIdMsgs = wsAndFileId2Msgs
                    .get(wsAndFileId);
            if (sameFileIdMsgs == null) {
                sameFileIdMsgs = new ArrayList<Message<FileFulltextOperations>>(
                        conf.getPullMaxMsgAtOneTime());
                wsAndFileId2Msgs.put(wsAndFileId, sameFileIdMsgs);
            }
            sameFileIdMsgs.add(m);
        }

        IdxTaskContext context = new IdxTaskContext();
        for (List<Message<FileFulltextOperations>> sameFileIdMsg : wsAndFileId2Msgs.values()) {
            consumerExecutor.asyncProcessMsg(client, sameFileIdMsg, context);
            context.incTaskCount();
        }
        context.waitAllTaskFinish();
    }

    private synchronized void recreateClient() throws MqException {
        closeClient();
        if (!isClosed) {
            client = createConsumerClient(topic);
        }

    }

    private void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    public synchronized void releaseResource() {
        isClosed = true;
        closeClient();
    }

    private ConsumerClient<FileFulltextOperations, FileFulltextOpFeedback> createConsumerClient(
            String topic) throws MqException {
        mqAdminClient.createTopicIfNotExist(topic, fulltextMqConfig.getTopicPartitionNum());
        mqAdminClient.createGroupIfNotExist(FulltextCommonDefine.FULLTEXT_GROUP_NAME, topic,
                ConsumerGroupOffsetEnum.OLDEST);
        return mqConsumerClientMgr.createClient(FulltextCommonDefine.FULLTEXT_GROUP_NAME,
                msgDeserializer, feedbackSerializer);
    }

}

class FileFulltextOperationsDeserializer implements MessageDeserializer<FileFulltextOperations> {

    private final ScmWorkspaceMgr wsMgr;

    FileFulltextOperationsDeserializer(ScmWorkspaceMgr wsMgr) {
        this.wsMgr = wsMgr;
    }

    @Override
    public FileFulltextOperations deserialize(BSONObject m) {
        FileFulltextOperations ret = new FileFulltextOperations();
        if (m instanceof BasicBSONList) {
            for (Object e : (BasicBSONList) m) {
                ret.add(convert((BSONObject) e));
            }
            return ret;
        }
        ret.add(convert(m));
        return ret;
    }

    private FileFulltextOperation convert(BSONObject m) {
        FileFulltextOperation ret = new FileFulltextOperation();
        ret.setFileId(BsonUtils.getStringChecked(m, FileFulltextOperation.KEY_FILE_ID));
        ret.setWsName(BsonUtils.getStringChecked(m, FileFulltextOperation.KEY_WS_NAME));
        ret.setOperationType(OperationType
                .valueOf(BsonUtils.getStringChecked(m, FileFulltextOperation.KEY_OPTION_TYPE)));
        ret.setIndexLocation(BsonUtils.getStringChecked(m, FileFulltextOperation.KEY_IDX_LOCATION));

        Boolean isSyncSaveIndex = BsonUtils.getBoolean(m,
                FileFulltextOperation.KEY_SYNC_SAVE_INDEX);
        if (isSyncSaveIndex == null) {
            // 这个字段表示在ES创建记录后，是否进行刷新（刷新后能被立刻检索到）
            // 消息没有这个字段表示这个消息是旧版内容服务上传文件时发来的，
            // 旧版消费者逻辑是根据工作区的索引模式决定是否在同步刷新创建的纪录
            // 这里我们根据工作区的索引模式填充这个字段
            ScmWorkspaceFulltextExtData wsExtData = wsMgr.getWorkspaceExtData(ret.getWsName());
            isSyncSaveIndex = wsExtData != null && wsExtData.getMode() == ScmFulltextMode.sync;
        }
        ret.setSyncSaveIndex(isSyncSaveIndex);
        ret.setReindex(BsonUtils.getBooleanOrElse(m, FileFulltextOperation.KEY_REINDEX, false));
        return ret;
    }
}