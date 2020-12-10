package com.sequoiacm.fulltext.server.consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.fulltext.server.config.FulltextMqConfig;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.sch.IdxTaskContext;
import com.sequoiacm.fulltext.server.workspace.ScmWorkspaceMgr;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.fulltext.common.FulltextCommonDefine;
import com.sequoiacm.infrastructure.fulltext.common.FulltextMsg;
import com.sequoiacm.infrastructure.fulltext.common.FulltextMsg.OptionType;
import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.client.core.ConsumerClient;
import com.sequoiacm.mq.client.core.ConsumerClientMgr;
import com.sequoiacm.mq.client.core.MessageDeseserializer;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroupOffsetEnum;
import com.sequoiacm.mq.core.module.Message;

public class IdxConsumerMainTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(IdxConsumerMainTask.class);
    private boolean isClosed = false;

    private static final MessageDeseserializer<FulltextMsg> deserializer = new MessageDeseserializer<FulltextMsg>() {
        @Override
        public FulltextMsg deserialize(BSONObject m) {
            FulltextMsg ret = new FulltextMsg();
            ret.setFileId(BsonUtils.getStringChecked(m, FulltextMsg.KEY_FILE_ID));
            ret.setWsName(BsonUtils.getStringChecked(m, FulltextMsg.KEY_WS_NAME));
            ret.setOptionType(
                    OptionType.valueOf(BsonUtils.getStringChecked(m, FulltextMsg.KEY_OPTION_TYPE)));
            ret.setIndexLocation(BsonUtils.getStringChecked(m, FulltextMsg.KEY_IDX_LOCATION));
            return ret;
        }
    };
    private final ScmWorkspaceMgr wsInfoMgr;
    private final FulltextMqConfig fulltextMqConfig;

    private IdxConsumerExecutor consumerExecutor;
    private AdminClient mqAdminClient;
    private ConsumerClientMgr mqConsumerClientMgr;
    private ConsumerClient<FulltextMsg> client;
    private String topic;

    public IdxConsumerMainTask(AdminClient mqAdminClient, ConsumerClientMgr consumerClientMgr,
            IdxConsumerExecutor consumerExecutor, ScmWorkspaceMgr wsInfoMgr,
            FulltextMqConfig fulltextMqConfig) {
        this.mqAdminClient = mqAdminClient;
        this.mqConsumerClientMgr = consumerClientMgr;
        this.consumerExecutor = consumerExecutor;
        this.topic = FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC;
        this.wsInfoMgr = wsInfoMgr;
        this.fulltextMqConfig = fulltextMqConfig;
    }

    @Override
    public void run() {
        while (true) {
            try {
                pullAndConsume();
            }
            catch (Exception e) {
                logger.error("consumer main task occur unexpected exception", e);
            }
        }
    }

    private void pullAndConsume() throws FullTextException, MqException, InterruptedException {
        if (client == null) {
            recreateClient();
        }

        List<Message<FulltextMsg>> msgs = null;
        try {
            msgs = client.pullMsg(5);
        }
        catch (MqException e) {
            logger.warn("failed to pull msg:topic=" + topic, e);
            recreateClient();
            return;
        }

        if (msgs == null) {
            Thread.sleep(500);
            return;
        }

        Map<String, List<FulltextMsg>> wsAndFileId2Msgs = new HashMap<>(5);
        for (Message<FulltextMsg> m : msgs) {
            String wsAndFileId = m.getKey();
            List<FulltextMsg> sameFileIdMsgs = wsAndFileId2Msgs.get(wsAndFileId);
            if (sameFileIdMsgs == null) {
                sameFileIdMsgs = new ArrayList<FulltextMsg>(5);
                wsAndFileId2Msgs.put(wsAndFileId, sameFileIdMsgs);
            }
            sameFileIdMsgs.add(m.getMsgContent());
        }

        IdxTaskContext context = new IdxTaskContext();
        for (List<FulltextMsg> sameFileIdMsg : wsAndFileId2Msgs.values()) {
            consumerExecutor.asyncProcessMsg(
                    wsInfoMgr.getWorkspaceExtData(sameFileIdMsg.get(0).getWsName()), sameFileIdMsg,
                    context);
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

    public synchronized void relaseResource() {
        isClosed = true;
        closeClient();
    }

    private ConsumerClient<FulltextMsg> createConsumerClient(String topic) throws MqException {
        mqAdminClient.createTopicIfNotExist(topic, fulltextMqConfig.getTopicPartitionNum());
        mqAdminClient.createGroupIfNotExist(FulltextCommonDefine.FULLTEXT_GROUP_NAME, topic,
                ConsumerGroupOffsetEnum.OLDEST);
        return mqConsumerClientMgr.createClient(FulltextCommonDefine.FULLTEXT_GROUP_NAME,
                deserializer);
    }

}
