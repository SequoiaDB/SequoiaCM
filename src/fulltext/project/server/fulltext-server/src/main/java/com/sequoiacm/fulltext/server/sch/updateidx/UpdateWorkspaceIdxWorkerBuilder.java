package com.sequoiacm.fulltext.server.sch.updateidx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.content.client.ContentserverClientMgr;
import com.sequoiacm.fulltext.server.ConfServiceClient;
import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.fileidx.FileIdxDaoFactory;
import com.sequoiacm.fulltext.server.lock.LockManager;
import com.sequoiacm.fulltext.server.lock.LockPathFactory;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobType;
import com.sequoiacm.fulltext.server.sch.IdxThreadPool;
import com.sequoiacm.fulltext.server.sch.createidx.CreateWorkspaceIdxWorkerConfig;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.mq.client.core.ProducerClient;
import com.sequoiacm.schedule.client.worker.ScheduleWorker;
import com.sequoiacm.schedule.client.worker.ScheduleWorkerBuilder;

@Component
public class UpdateWorkspaceIdxWorkerBuilder implements ScheduleWorkerBuilder {
    @Autowired
    private ContentserverClientMgr csMgr;
    @Autowired
    private ScmSiteInfoMgr siteInfoMgr;
    @Autowired
    private EsClient esClient;
    @Autowired
    private ConfServiceClient confClient;

    @Autowired
    private LockManager lockManager;
    @Autowired
    private LockPathFactory lockPathFactory;
    @Autowired
    private AdminClient mqAdmin;
    @Autowired
    private IdxThreadPool idxThreadPool;
    @Autowired
    private CreateWorkspaceIdxWorkerConfig conf;
    @Autowired
    private ProducerClient producerClient;
    @Autowired
    private FileIdxDaoFactory scmFileIdDaoFactory;

    @Override
    public String getJobType() {
        return FulltextIdxSchJobType.FULLTEXT_INDEX_UPDATE.name();
    }

    @Override
    public ScheduleWorker createWorker() {
        return new UpdateWorkspaceIdxWorker(scmFileIdDaoFactory, conf, esClient, csMgr, siteInfoMgr, confClient, lockManager,
                lockPathFactory, mqAdmin, idxThreadPool, producerClient);
    }

}
