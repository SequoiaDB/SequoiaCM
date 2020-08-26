package com.sequoiacm.fulltext.server.es;

import org.bson.BSONObject;
import org.springframework.stereotype.Component;

import com.sequoiacm.fulltext.server.exception.FullTextException;

@Component
public interface EsClient {

    public EsDoumentCursor search(String index, BSONObject queryBody) throws FullTextException;

    public void deleteAsyncByDocId(String index, String docid) throws FullTextException;

    public void deleteAsyncByFileId(String index, String fileId) throws FullTextException;

    public void refreshIndexSilence(String index);

    public String index(String index, EsDocument document, boolean syncIndex)
            throws FullTextException;

    public void dropIndex(String index) throws FullTextException;

    public void dropIndexAsync(String index) throws FullTextException;

    public void createIndexWithOverwrite(String index) throws FullTextException;
    
    public void createIndexIfNotExist(String index) throws FullTextException;
}
