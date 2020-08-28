package com.sequoiacm.fulltext.server.sch.createidx;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.content.client.ContentserverClient;
import com.sequoiacm.content.client.ContentserverClientMgr;
import com.sequoiacm.content.client.ScmEleCursor;
import com.sequoiacm.content.client.model.ScmFileInfo;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.fulltext.server.ConfServiceClient;
import com.sequoiacm.fulltext.server.WsFulltextExtDataModifier;
import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.lock.LockManager;
import com.sequoiacm.fulltext.server.lock.LockPathFactory;
import com.sequoiacm.fulltext.server.parser.TextualParserMgr;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobData;
import com.sequoiacm.fulltext.server.sch.IdxThreadPool;
import com.sequoiacm.fulltext.server.sch.IdxWorkerBase;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.infrastructure.fulltext.common.ScmFileFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.schedule.common.model.ScheduleException;

public class IdxCreateWorker extends IdxWorkerBase {
    protected ContentserverClientMgr csMgr;
    protected ScmSiteInfoMgr siteInfoMgr;
    protected EsClient esClient;
    protected TextualParserMgr textParserMgr;
    protected ConfServiceClient confClient;
    protected FulltextIdxSchJobData jobData;

    protected LockManager lockMgr;
    protected LockPathFactory lockPathFactory;

    public IdxCreateWorker(EsClient esClient, ContentserverClientMgr csMgr,
            TextualParserMgr textualParserMgr, ScmSiteInfoMgr siteInfoMgr,
            ConfServiceClient confClient, LockManager lockMgr, LockPathFactory lockPathFactory,
            IdxThreadPool idxThreadPool) {
        super(idxThreadPool);
        this.esClient = esClient;
        this.csMgr = csMgr;
        this.textParserMgr = textualParserMgr;
        this.siteInfoMgr = siteInfoMgr;
        this.confClient = confClient;
        this.lockMgr = lockMgr;
        this.lockPathFactory = lockPathFactory;
    }

    protected void createIndex(FulltextIdxSchJobData data, ContentserverClient csClient,
            BSONObject wsFulltextFileMatcher) throws ScmServerException, ScheduleException {
        //拼上$ne CREATED，同时保证先建历史文件的索引，如果历史建立失败，最新文件不建
        BasicBSONList andArr = new BasicBSONList();
        andArr.add(wsFulltextFileMatcher);
        BasicBSONObject neq = new BasicBSONObject("$ne", ScmFileFulltextStatus.CREATED.name());
        andArr.add(new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA + "."
                + ScmFileFulltextExtData.FIELD_IDX_STATUS, neq));
        BasicBSONObject condition = new BasicBSONObject("$and", andArr);

        ScmEleCursor<ScmFileInfo> cursor = csClient.listFile(data.getWs(), condition,
                CommonDefine.Scope.SCOPE_CURRENT, null, 0, -1);
        try {
            while (cursor.hasNext()) {
                ScmFileInfo file = cursor.getNext();
                IdxCreateDao idxCreator = IdxCreateDao
                        .newBuilder(esClient, csMgr, textParserMgr, siteInfoMgr)
                        .file(data.getWs(), file.getId()).indexLocation(data.getIndexDataLocation())
                        .syncIndexInEs(false).get();
                IdxCreateTask task = new IdxCreateTask(idxCreator, getTaskContext());
                submit(task);
                getTaskContext().incTaskCount();
                reportStatusSlience(false);
            }
        }
        finally {
            cursor.close();
        }
    }

    @Override
    protected void exec(String schName, BSONObject jobDataBson) throws Exception {
        jobData = new FulltextIdxSchJobData(jobDataBson);
        ContentserverClient csClient = csMgr.getClient(siteInfoMgr.getRootSiteName());

        long estimateCount = csClient.countFile(jobData.getWs(), CommonDefine.Scope.SCOPE_CURRENT,
                jobData.getFileMatcher());
        getStatus().setEstimateCount(estimateCount);
        reportInitStatus();

        createIndex(jobData, csClient, jobData.getFileMatcher());

        getTaskContext().waitAllTaskFinish();
        
        esClient.refreshIndexSilence(jobData.getIndexDataLocation());

        WsFulltextExtDataModifier modifier = new WsFulltextExtDataModifier(jobData.getWs(),
                schName);
        modifier.setIndexStatus(ScmFulltextStatus.CREATED);

        ScmLock lock = lockMgr.acquiresLock(lockPathFactory.fulltextLockPath(jobData.getWs()));
        try {
            confClient.updateWsExternalData(modifier);
        }
        finally {
            lock.unlock();
        }

        reportStatusSlience(true);
    }

    @Override
    public String toString() {
        return "index create, jobData=" + jobData;
    }
}
