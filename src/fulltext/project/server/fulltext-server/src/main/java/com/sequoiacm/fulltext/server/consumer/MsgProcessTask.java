package com.sequoiacm.fulltext.server.consumer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.fulltext.server.fileidx.FileIdxDao;
import com.sequoiacm.fulltext.server.fileidx.FileIdxDaoFactory;
import com.sequoiacm.fulltext.server.sch.IdxTaskContext;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOpFeedback;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOperation;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOperations;
import com.sequoiacm.mq.client.core.ConsumerClient;
import com.sequoiacm.mq.core.module.Message;

public class MsgProcessTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MsgProcessTask.class);
    private final FileIdxDaoFactory scmFileIdxFactory;

    private final List<Message<FileFulltextOperations>> msgs;
    private final ConsumerClient<FileFulltextOperations, FileFulltextOpFeedback> msgClient;
    private final IdxTaskContext context;

    public MsgProcessTask(FileIdxDaoFactory scmFileIdxFactory,
            ConsumerClient<FileFulltextOperations, FileFulltextOpFeedback> msgClient,
            List<Message<FileFulltextOperations>> msgs, IdxTaskContext context) {
        this.msgs = msgs;
        this.context = context;
        this.msgClient = msgClient;
        this.scmFileIdxFactory = scmFileIdxFactory;
    }

    @Override
    public void run() {
        try {
            for (Message<FileFulltextOperations> m : msgs) {
                try {
                    processMsg(m);
                }
                catch (Throwable e) {
                    logger.error("failed to process msg:{}", m, e);
                }
            }
        }
        finally {
            context.reduceTaskCount();
        }
    }

    private void processMsg(Message<FileFulltextOperations> msg) {
        logger.debug("processing msg:{}", msg);

        FileFulltextOperations ops = msg.getMsgContent();
        int successCount = 0;
        int failedCount = 0;
        for (FileFulltextOperation op : ops) {
            FileIdxDao dao = null;
            try {
                dao = scmFileIdxFactory.createDao(op);
                dao.process();
                successCount += dao.processFileCount();
            }
            catch (Throwable e) {
                logger.error("failed to process msg:topic={}, key={}, id={}, op={}", msg.getTopic(),
                        msg.getKey(), msg.getId(), op, e);
                if (dao == null) {
                    failedCount++;
                }
                else {
                    failedCount += dao.processFileCount();
                }
            }
        }

        if (msg.getMsgProducer() != null) {
            FileFulltextOpFeedback feedback = new FileFulltextOpFeedback(successCount, failedCount);
            msgClient.feedbackSilence(msg, feedback);
        }
    }

}
