package com.sequoiacm.fulltext.server.consumer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.content.client.ContentserverClientMgr;
import com.sequoiacm.fulltext.server.config.FulltextMqConfig;
import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.parser.TextualParserMgr;
import com.sequoiacm.fulltext.server.sch.IdxTaskContext;
import com.sequoiacm.fulltext.server.sch.IdxThreadPoolConfig;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.fulltext.server.workspace.ScmWorkspaceMgr;
import com.sequoiacm.infrastructure.fulltext.common.FulltextMsg;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.client.core.ConsumerClientMgr;

@Component
public class IdxConsumerExecutor {
    private static final Logger logger = LoggerFactory.getLogger(IdxConsumerExecutor.class);
    private final ScmWorkspaceMgr wsMgr;
    @Autowired
    private ContentserverClientMgr csMgr;
    @Autowired
    private TextualParserMgr textualParserMgr;
    @Autowired
    private EsClient esClient;
    @Autowired
    private ScmSiteInfoMgr siteInfoMgr;

    private ThreadPoolExecutor taskMgr;

    private Map<String, IdxConsumerMainTask> ws2Consumer = new ConcurrentHashMap<>();
    private AdminClient mqAdminClient;
    private ConsumerClientMgr consumerClientMgr;
    private IdxConsumerMainTask idxConsumerMainTask;

    @Autowired
    public IdxConsumerExecutor(AdminClient mqAdminClient, ConsumerClientMgr consumerClientMgr,
            IdxThreadPoolConfig threadPoolConfig, ScmWorkspaceMgr wsMgr,
            FulltextMqConfig fulltextMqConfig) {
        this.mqAdminClient = mqAdminClient;
        this.consumerClientMgr = consumerClientMgr;
        this.wsMgr = wsMgr;
        taskMgr = new ThreadPoolExecutor(threadPoolConfig.getCorePoolSize(),
                threadPoolConfig.getMaxPoolSize(), threadPoolConfig.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(threadPoolConfig.getBlockingQueueSize()),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
        idxConsumerMainTask = new IdxConsumerMainTask(mqAdminClient, consumerClientMgr, this, wsMgr,
                fulltextMqConfig);
        taskMgr.submit(idxConsumerMainTask);
    }

    @PreDestroy
    public void destroy() {
        if (taskMgr != null) {
            taskMgr.shutdownNow();
        }
        if (idxConsumerMainTask != null) {
            idxConsumerMainTask.relaseResource();
        }

    }

    void asyncProcessMsg(ScmWorkspaceFulltextExtData wsExtData, List<FulltextMsg> msgs,
            IdxTaskContext context) throws FullTextException {
        MsgProcessTask task = new MsgProcessTask(esClient, csMgr, textualParserMgr, siteInfoMgr,
                msgs, wsExtData, context);
        taskMgr.submit(task);
    }
}
