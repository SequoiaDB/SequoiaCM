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
import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.parser.TextualParserMgr;
import com.sequoiacm.fulltext.server.sch.IdxTaskContext;
import com.sequoiacm.fulltext.server.sch.IdxThreadPoolConfig;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.fulltext.server.workspace.ScmWorkspaceEventListener;
import com.sequoiacm.fulltext.server.workspace.ScmWorkspaceInfo;
import com.sequoiacm.infrastructure.fulltext.common.FulltextCommonDefine;
import com.sequoiacm.infrastructure.fulltext.common.FulltextMsg;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.client.core.ConsumerClientMgr;

@Component
public class IdxConsumerExecutor implements ScmWorkspaceEventListener {
    private static final Logger logger = LoggerFactory.getLogger(IdxConsumerExecutor.class);
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

    @Autowired
    public IdxConsumerExecutor(AdminClient mqAdminClient, ConsumerClientMgr consumerClientMgr,
            IdxThreadPoolConfig threadPoolConfig) {
        this.mqAdminClient = mqAdminClient;
        this.consumerClientMgr = consumerClientMgr;
        taskMgr = new ThreadPoolExecutor(threadPoolConfig.getCorePoolSize(),
                threadPoolConfig.getMaxPoolSize(), threadPoolConfig.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(threadPoolConfig.getBlockingQueueSize()),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @PreDestroy
    public void destory() {
        if (taskMgr != null) {
            taskMgr.shutdownNow();
        }
    }

    void asyncProcessMsg(ScmWorkspaceFulltextExtData wsExtData, List<FulltextMsg> msgs,
            IdxTaskContext context) throws FullTextException {
        MsgProcessTask task = new MsgProcessTask(esClient, csMgr, textualParserMgr, siteInfoMgr,
                msgs, wsExtData, context);
        taskMgr.submit(task);
    }

    @Override
    public synchronized void onWorkspaceAdd(ScmWorkspaceInfo ws) {
        if (ws.getExternalData().isEnabled()) {
            IdxConsumerMainTask task = new IdxConsumerMainTask(mqAdminClient, consumerClientMgr,
                    this, ws.getExternalData());
            taskMgr.submit(task);
            ws2Consumer.put(ws.getName(), task);
            return;
        }
        IdxConsumerMainTask task = ws2Consumer.remove(ws.getName());
        if (task != null) {
            task.stop();
        }

    }

    @Override
    public synchronized void onWorkspaceRemove(String ws) {
        IdxConsumerMainTask task = ws2Consumer.remove(ws);
        if (task != null) {
            task.stop();
        }
        try {
            mqAdminClient.deleteTopic(ws + FulltextCommonDefine.FULLTEXT_TOPIC_TAIL);
        }
        catch (Exception e) {
            logger.warn("failed to remove topic:{}", ws + FulltextCommonDefine.FULLTEXT_TOPIC_TAIL,
                    e);
        }
    }

    @Override
    public synchronized void onWorkspaceUpdate(ScmWorkspaceInfo newWs) {
        if (newWs.getExternalData().isEnabled()) {
            IdxConsumerMainTask task = ws2Consumer.get(newWs.getName());
            if (task == null) {
                onWorkspaceAdd(newWs);
                return;
            }
            task.updateWsExtData(newWs.getExternalData());
            return;
        }

        onWorkspaceRemove(newWs.getName());
    }
}
