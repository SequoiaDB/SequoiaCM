package com.sequoiacm.fulltext.server.consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.sch.IdxTaskContext;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.fulltext.common.FulltextCommonDefine;
import com.sequoiacm.infrastructure.fulltext.common.FulltextMsg;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
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

    private IdxConsumerExecutor consumerExecutor;
    private AdminClient mqAdminClient;
    private ConsumerClientMgr mqConsumerClientMgr;
    private ConsumerClient<FulltextMsg> client;
    private String topic;

    private volatile ScmWorkspaceFulltextExtData ws;

    private volatile boolean stopFlag;

    public IdxConsumerMainTask(AdminClient mqAdminClient, ConsumerClientMgr consumerClientMgr,
            IdxConsumerExecutor consumerExecutor, ScmWorkspaceFulltextExtData ws) {
        this.mqAdminClient = mqAdminClient;
        this.mqConsumerClientMgr = consumerClientMgr;
        this.consumerExecutor = consumerExecutor;
        this.ws = ws;
        this.topic = ws.getWsName() + FulltextCommonDefine.FULLTEXT_TOPIC_TAIL;
    }

    public void updateWsExtData(ScmWorkspaceFulltextExtData newWsExtData) {
        this.ws = newWsExtData;
    }

    public synchronized void stop() {
        this.stopFlag = true;
        if (client != null) {
            client.close();
        }
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
            if (stopFlag) {
                logger.info("consumer task exit:topic={}", topic);
                return;
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

        Map<String, List<FulltextMsg>> fileId2Msgs = new HashMap<>(5);
        for (Message<FulltextMsg> m : msgs) {
            String fileId = m.getMsgContent().getFileId();
            List<FulltextMsg> sameFileIdMsgs = fileId2Msgs.get(fileId);
            if (sameFileIdMsgs == null) {
                sameFileIdMsgs = new ArrayList<FulltextMsg>(5);
                fileId2Msgs.put(fileId, sameFileIdMsgs);
            }
            sameFileIdMsgs.add(m.getMsgContent());
        }

        IdxTaskContext contex = new IdxTaskContext();
        for (List<FulltextMsg> sameFileIdMsg : fileId2Msgs.values()) {
            consumerExecutor.asyncProcessMsg(ws, sameFileIdMsg, contex);
            contex.incTaskCount();
        }

        contex.waitAllTaskFinish();
    }

    private synchronized void recreateClient() throws MqException {
        if (client != null) {
            client.close();
        }
        if (stopFlag) {
            return;
        }
        client = createConsumerClient(topic);
    }

    private ConsumerClient<FulltextMsg> createConsumerClient(String topic) throws MqException {
        String groupName = "fulltext-server-" + topic;
        mqAdminClient.createGroupIfNotExist("fulltext-server-" + topic, topic,
                ConsumerGroupOffsetEnum.OLDEST);
        return mqConsumerClientMgr.createClient(groupName, deserializer);
    }

}
