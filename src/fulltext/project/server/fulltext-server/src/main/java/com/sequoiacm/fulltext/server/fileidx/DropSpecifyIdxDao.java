package com.sequoiacm.fulltext.server.fileidx;

import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.exception.FullTextException;

class DropSpecifyIdxDao extends FileIdxDao {

    private final String idxDocId;
    private EsClient esClient;

    DropSpecifyIdxDao(String ws, String fileId, String esIdxLocation, EsClient esClient,
            String idxDocId) {
        super(ws, fileId, esIdxLocation);
        this.esClient = esClient;
        this.idxDocId = idxDocId;
    }

    @Override
    public int processFileCount() {
        return 1;
    }

    public void process() throws FullTextException {
        esClient.deleteAsyncByDocId(getEsIdxLocation(), idxDocId);
    }

}
