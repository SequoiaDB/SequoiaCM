package com.sequoiacm.fulltext.server.consumer;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import com.sequoiacm.fulltext.es.client.base.EsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.fulltext.server.config.FulltextMqConfig;
import com.sequoiacm.fulltext.server.fileidx.FileIdxDaoFactory;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.parser.TextualParserMgr;
import com.sequoiacm.fulltext.server.sch.IdxTaskContext;
import com.sequoiacm.fulltext.server.workspace.ScmWorkspaceMgr;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOpFeedback;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOperations;
import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.client.core.ConsumerClient;
import com.sequoiacm.mq.client.core.ConsumerClientMgr;
import com.sequoiacm.mq.core.module.Message;

@Component
public class IdxConsumerExecutor {
    private static final Logger logger = LoggerFactory.getLogger(IdxConsumerExecutor.class);

    private ThreadPoolExecutor taskMgr;
    private IdxConsumerMainTask idxConsumerMainTask;
    private final FileIdxDaoFactory scmFileIdxDaoFactory;

    @Autowired
    public IdxConsumerExecutor(AdminClient mqAdminClient, ConsumerClientMgr consumerClientMgr,
            IdxConsumerConfig consumerConfig, ScmWorkspaceMgr wsMgr,
            FulltextMqConfig fulltextMqConfig, FileIdxDaoFactory scmFileIdxDaoFactory) {
        this.scmFileIdxDaoFactory = scmFileIdxDaoFactory;
        taskMgr = new ThreadPoolExecutor(consumerConfig.getCorePoolSize(),
                consumerConfig.getMaxPoolSize(), consumerConfig.getCoreThreadKeepAliveTime(),
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(consumerConfig.getBlockingQueueSize()),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
        idxConsumerMainTask = new IdxConsumerMainTask(consumerConfig, mqAdminClient,
                consumerClientMgr, this, wsMgr, fulltextMqConfig);
        taskMgr.submit(idxConsumerMainTask);
    }

    @PreDestroy
    public void destroy() {
        if (taskMgr != null) {
            taskMgr.shutdownNow();
        }
        if (idxConsumerMainTask != null) {
            idxConsumerMainTask.releaseResource();
        }

    }

    void asyncProcessMsg(ConsumerClient<FileFulltextOperations, FileFulltextOpFeedback> msgClient,
            List<Message<FileFulltextOperations>> msgs, IdxTaskContext context) {
        MsgProcessTask task = new MsgProcessTask(scmFileIdxDaoFactory, msgClient, msgs, context);
        taskMgr.submit(task);
    }
}
