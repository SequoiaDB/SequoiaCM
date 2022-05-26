package com.sequoiacm.fulltext.server.fileidx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.content.client.ContentserverClientMgr;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.fulltext.server.parser.TextualParserMgr;
import com.sequoiacm.fulltext.server.site.ScmSiteInfoMgr;
import com.sequoiacm.infrastructure.fulltext.common.FileFulltextOperation;

@Component
public class FileIdxDaoFactory {

    @Autowired
    protected ContentserverClientMgr csMgr;
    @Autowired
    protected TextualParserMgr textualParserMgr;
    @Autowired
    protected EsClient esClient;
    @Autowired
    protected ScmSiteInfoMgr siteInfoMgr;

    public FileIdxDao createDao(FileFulltextOperation op) throws FullTextException {
        switch (op.getOperationType()) {
            case CREATE_IDX:
                return new CreateFileIdxDao(op.getWsName(), op.getFileId(), op.getIndexLocation(),
                        op.isSyncSaveIndex(), op.isReindex(), esClient, csMgr, textualParserMgr,
                        siteInfoMgr);
            case DROP_IDX_AND_UPDATE_FILE:
                return new DropAndUpdateFileIdxDao(op.getWsName(), op.getFileId(),
                        op.getIndexLocation(), csMgr, siteInfoMgr, esClient);
            case DROP_IDX_ONLY:
                return new DropOnlyFileIdxDao(op.getWsName(), op.getFileId(), op.getIndexLocation(),
                        esClient);
            case DROP_SPECIFY_IDX_ONLY:
                return new DropSpecifyIdxDao(op.getWsName(), op.getFileId(), op.getIndexLocation(),
                        esClient, op.getIndexDocId());
            default:
                throw new FullTextException(ScmError.SYSTEM_ERROR,
                        "no such FileIdxDao:" + op.getOperationType());
        }
    }
}
