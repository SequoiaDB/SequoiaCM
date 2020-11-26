package com.sequoiacm.fulltext.server.consumer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.content.client.ContentserverClient;
import com.sequoiacm.content.client.ContentserverClientMgr;
import com.sequoiacm.exception.ScmServerException;
import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.parser.TextualParserMgr;
import com.sequoiacm.fulltext.server.sch.IdxTaskContext;
import com.sequoiacm.fulltext.server.sch.createidx.IdxCreateDao;
import com.sequoiacm.fulltext.server.sch.updateidx.IdxDropAndUpdateDao;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.infrastructure.fulltext.common.FulltextMsg;
import com.sequoiacm.infrastructure.fulltext.common.ScmWorkspaceFulltextExtData;
import com.sequoiacm.infrastructure.fulltext.common.FulltextMsg.OptionType;
import com.sequoiacm.infrastructure.fulltext.core.ScmFulltextMode;

public class MsgProcessTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MsgProcessTask.class);

    private List<FulltextMsg> msgs;
    private ScmWorkspaceFulltextExtData wsExtData;
    private EsClient esClient;
    private ContentserverClientMgr csMgr;
    private TextualParserMgr textualParserMgr;
    private ScmSiteInfoMgr siteInfoMgr;

    private IdxTaskContext context;

    public MsgProcessTask(EsClient esClient, ContentserverClientMgr csMgr,
            TextualParserMgr textualParserMgr, ScmSiteInfoMgr siteInfoMgr, List<FulltextMsg> msgs,
            ScmWorkspaceFulltextExtData wsExtData, IdxTaskContext context) {
        this.esClient = esClient;
        this.csMgr = csMgr;
        this.textualParserMgr = textualParserMgr;
        this.siteInfoMgr = siteInfoMgr;

        this.msgs = msgs;
        this.wsExtData = wsExtData;

        this.context = context;
    }

    @Override
    public void run() {
        try {
            for (FulltextMsg m : msgs) {
                try {
                    processMsg(m);
                }
                catch (Throwable e) {
                    logger.error("failed to proccess msg:{}", m, e);
                }
            }
        }
        finally {
            context.reduceTaskCount();
        }
    }

    private void processMsg(FulltextMsg msg) throws FullTextException, ScmServerException {
        logger.debug("processing msg:{}", msg);
        boolean syncIndex = false;
        if (wsExtData != null && wsExtData.getMode() == ScmFulltextMode.sync) {
            syncIndex = true;
        }
        if (msg.getOptionType() == OptionType.CREATE_IDX) {
            IdxCreateDao idxCreator = IdxCreateDao
                    .newBuilder(esClient, csMgr, textualParserMgr, siteInfoMgr)
                    .file(msg.getWsName(), msg.getFileId()).indexLocation(msg.getIndexLocation())
                    .syncIndexInEs(syncIndex).get();
            idxCreator.createIdx();
            return;
        }

        if (msg.getOptionType() == OptionType.DROP_IDX_AND_UPDATE_FILE) {
            String rootSite = siteInfoMgr.getRootSiteName();
            ContentserverClient csClient = csMgr.getClient(rootSite);
            IdxDropAndUpdateDao dao = IdxDropAndUpdateDao.newBuilder(csClient, esClient)
                    .file(msg.getWsName(), msg.getFileId()).indexLocation(msg.getIndexLocation())
                    .get();
            dao.dropAndUpdate();
            return;
        }

        if (msg.getOptionType() == OptionType.DROP_IDX_ONLY) {
            IdxDropDao dao = IdxDropDao.newBuilder(esClient).file(msg.getFileId())
                    .indexLocation(msg.getIndexLocation()).get();
            dao.drop();
            return;
        }
    }

}
