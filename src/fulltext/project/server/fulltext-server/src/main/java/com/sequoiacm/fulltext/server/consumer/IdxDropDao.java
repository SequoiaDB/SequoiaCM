package com.sequoiacm.fulltext.server.consumer;

import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.exception.FullTextException;

public class IdxDropDao {
    private String indexLocation;

    private String fileId;

    private EsClient esClient;

    public static Builder newBuilder(EsClient esClient) {
        return new Builder(esClient);
    }

    public static class Builder {
        private IdxDropDao dao;

        private Builder(EsClient esClient) {
            dao = new IdxDropDao(esClient);
        }

        public Builder file(String fileId) {
            dao.fileId = fileId;
            return this;
        }

        public Builder indexLocation(String indexLocation) {
            dao.indexLocation = indexLocation;
            return this;
        }

        public IdxDropDao get() {
            return dao;
        }
    }

    private IdxDropDao(EsClient esClient) {

        this.esClient = esClient;
    }

    public void drop() throws FullTextException {
        esClient.deleteAsyncByFileId(indexLocation, fileId);
    }

}
