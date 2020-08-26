package com.sequoiacm.fulltext.server.sch.createidx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.content.client.ContentserverClientMgr;
import com.sequoiacm.fulltext.server.ConfServiceClient;
import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.lock.LockManager;
import com.sequoiacm.fulltext.server.lock.LockPathFactory;
import com.sequoiacm.fulltext.server.parser.TextualParserMgr;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobType;
import com.sequoiacm.fulltext.server.sch.IdxThreadPool;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.schedule.client.worker.ScheduleWorker;
import com.sequoiacm.schedule.client.worker.ScheduleWorkerBuilder;

@Component
public class IdxCreateWorkerFactory implements ScheduleWorkerBuilder {
    @Autowired
    private ContentserverClientMgr csMgr;
    @Autowired
    private ScmSiteInfoMgr siteInfoMgr;
    @Autowired
    private EsClient esClient;
    @Autowired
    private TextualParserMgr textParserMgr;
    @Autowired
    private ConfServiceClient confClient;

    @Autowired
    private LockManager lockManager;
    @Autowired
    private LockPathFactory lockPathFactory;
    @Autowired
    private IdxThreadPool idxThreadPool;

    @Override
    public String getJobType() {
        return FulltextIdxSchJobType.FULLTEXT_INDEX_CREATE.name();
    }

    @Override
    public ScheduleWorker createWorker() {
        return new IdxCreateWorker(esClient, csMgr, textParserMgr, siteInfoMgr, confClient,
                lockManager, lockPathFactory, idxThreadPool);
    }
}
