package com.sequoiacm.fulltext.server.es;

import java.util.List;

import com.sequoiacm.fulltext.server.exception.FullTextException;

public interface EsDoumentCursor {

    public List<EsSearchRes> getNextBatch() throws FullTextException;

    public void close();
}
