package com.sequoiacm.fulltext.es.client.base;

import org.bson.BSONObject;

import com.sequoiacm.fulltext.server.exception.FullTextException;

import javax.annotation.PreDestroy;

public interface EsClient {

    EsDocumentCursor search(String index, BSONObject queryBody) throws FullTextException;

    void deleteAsyncByDocId(String index, String docid) throws FullTextException;

    void deleteAsyncByFileId(String index, String fileId) throws FullTextException;

    void refreshIndexSilence(String index);

    String index(String index, EsDocument document, boolean syncIndex) throws FullTextException;

    void dropIndex(String index) throws FullTextException;

    void dropIndexAsync(String index) throws FullTextException;

    void createIndexWithOverwrite(String index) throws FullTextException;

    void createIndexIfNotExist(String index) throws FullTextException;

    @PreDestroy
    void destroy() throws Exception;
}
