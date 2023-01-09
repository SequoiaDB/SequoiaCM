package com.sequoiacm.fulltext.es.client.base;

import java.util.List;

import com.sequoiacm.fulltext.server.exception.FullTextException;

public interface EsDocumentCursor {

    public List<EsSearchRes> getNextBatch() throws FullTextException;

    public void close();
}
