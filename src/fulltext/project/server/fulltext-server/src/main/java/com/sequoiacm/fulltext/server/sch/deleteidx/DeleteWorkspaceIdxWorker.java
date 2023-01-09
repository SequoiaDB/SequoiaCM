package com.sequoiacm.fulltext.server.sch.deleteidx;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.content.client.ContentserverClient;
import com.sequoiacm.content.client.ContentserverClientMgr;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.es.client.base.EsClient;
import com.sequoiacm.fulltext.server.ConfServiceClient;
import com.sequoiacm.fulltext.server.WsFulltextExtDataModifier;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.lock.LockManager;
import com.sequoiacm.fulltext.server.lock.LockPathFactory;
import com.sequoiacm.fulltext.server.sch.FulltextIdxSchJobData;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.infrastructure.fulltext.common.FulltextCommonDefine;
import com.sequoiacm.infrastructure.fulltext.common.ScmFileFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.core.ScmFileFulltextStatus;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextStatus;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.mq.client.config.AdminClient;
import com.sequoiacm.schedule.client.worker.ScheduleWorker;

public class DeleteWorkspaceIdxWorker extends ScheduleWorker {
    private AdminClient mqAdmin;
    private ContentserverClientMgr csMgr;
    private EsClient esClient;
    private ScmSiteInfoMgr siteInfoMgr;
    private ConfServiceClient confClient;

    private LockManager lockMgr;
    private LockPathFactory lockPathFactory;

    public DeleteWorkspaceIdxWorker(AdminClient mqAdmin, ContentserverClientMgr csMgr, EsClient esClient,
            ScmSiteInfoMgr siteInfoMgr, ConfServiceClient confClient, LockManager lockMgr,
            LockPathFactory lockPathFactory) {
        this.mqAdmin = mqAdmin;
        this.csMgr = csMgr;
        this.esClient = esClient;
        this.siteInfoMgr = siteInfoMgr;
        this.confClient = confClient;
        this.lockMgr = lockMgr;
        this.lockPathFactory = lockPathFactory;
    }

    @Override
    protected void exec(String schName, BSONObject jobData) throws Exception {
        FulltextIdxSchJobData data = new FulltextIdxSchJobData(jobData);
        boolean isConsumed = mqAdmin.waitForMsgConsumed(FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC, FulltextCommonDefine.FULLTEXT_GROUP_NAME, data.getLatestMsgId(), true, Integer.MAX_VALUE, 5000);
        if (!isConsumed) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to wait msg to be consumed, timeout: topic="
                            + FulltextCommonDefine.FILE_FULLTEXT_OP_TOPIC + ", msgId="
                            + data.getLatestMsgId());
        }
        esClient.dropIndexAsync(data.getIndexDataLocation());
        ContentserverClient csClient = csMgr.getClient(siteInfoMgr.getRootSite().getName());

        ScmFileFulltextExtData newExtData = new ScmFileFulltextExtData(null,
                ScmFileFulltextStatus.NONE, null);
        BSONObject notEqNone = new BasicBSONObject("$ne", ScmFileFulltextStatus.NONE.name());
        BasicBSONObject matcher = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_EXTERNAL_DATA
                + "." + ScmFileFulltextExtData.FIELD_IDX_STATUS, notEqNone);
        csClient.updateFileExternalData(data.getWs(), matcher, newExtData.toBson());

        WsFulltextExtDataModifier modifier = new WsFulltextExtDataModifier(data.getWs(), schName);
        modifier.setFulltextJobName(null);
        modifier.setIndexStatus(ScmFulltextStatus.NONE);
        modifier.setIndexDataLocation(null);
        ScmLock lock = lockMgr.acquiresLock(lockPathFactory.fulltextLockPath(data.getWs()));
        try {
            confClient.updateWsExternalData(modifier);
        }
        finally {
            lock.unlock();
        }
    }

}
