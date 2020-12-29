package com.sequoiacm.fulltext.server.fileidx;

import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.exception.FullTextException;


class DropOnlyFileIdxDao extends FileIdxDao {

    private EsClient esClient;

    DropOnlyFileIdxDao(String ws, String fileId, String esIdxLocation, EsClient esClient) {
        super(ws, fileId, esIdxLocation);
        this.esClient = esClient;
    }

    @Override
    public int processFileCount() {
        return 1;
    }

    public void process() throws FullTextException {
        esClient.deleteAsyncByFileId(getEsIdxLocation(), getFileId());
    }

}
