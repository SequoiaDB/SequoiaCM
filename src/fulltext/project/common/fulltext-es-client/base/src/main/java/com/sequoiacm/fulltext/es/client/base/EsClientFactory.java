package com.sequoiacm.fulltext.es.client.base;

import com.sequoiacm.fulltext.server.exception.FullTextException;

public interface EsClientFactory {
    EsClient createEsClient(EsClientConfig config) throws FullTextException;
}
