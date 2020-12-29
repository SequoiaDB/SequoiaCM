package com.sequoiacm.fulltext.server.sch.deleteidx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.content.client.ContentserverClientMgr;
import com.sequoiacm.fulltext.server.ConfServiceClient;
import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.lock.LockManager;
import com.sequoiacm.fulltext.server.lock.LockPathFactory;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobType;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.schedule.client.worker.ScheduleWorker;
import com.sequoiacm.schedule.client.worker.ScheduleWorkerBuilder;

@Component
public class DeleteWorkspaceIdxWorkerBuilder implements ScheduleWorkerBuilder {
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

    @Override
    public String getJobType() {
        return FulltextIdxSchJobType.FULLTEXT_INDEX_DELETE.name();
    }

    @Override
    public ScheduleWorker createWorker() {
        return new DeleteWorkspaceIdxWorker(mqAdmin, csMgr, esClient, siteInfoMgr, confClient, lockManager,
                lockPathFactory);
    }

}
