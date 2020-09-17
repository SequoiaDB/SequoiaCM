package com.sequoiacm.fulltext.server.es.es_6_3_2;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.rest.RestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.es.EsClient;
import com.sequoiacm.fulltext.server.es.EsClientConfig;
import com.sequoiacm.fulltext.server.es.EsDocument;
import com.sequoiacm.fulltext.server.es.EsDoumentCursor;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.fulltext.common.FulltextDocDefine;

@Component
class EsClientImpl implements EsClient {
    private static final Logger logger = LoggerFactory.getLogger(EsClientImpl.class);

    private RestHighLevelClient restClient;

    private EsClientConfig esConfig;

    @Autowired
    public EsClientImpl(EsClientConfig esConfig) throws MalformedURLException {
        this.esConfig = esConfig;
        List<HttpHost> httpHosts = new ArrayList<>();
        for (String url : esConfig.getUrls()) {
            URL u = new URL(url);
            httpHosts.add(new HttpHost(u.getHost(), u.getPort(), u.getProtocol()));
        }
        restClient = new RestHighLevelClient(
                RestClient.builder(httpHosts.toArray(new HttpHost[httpHosts.size()])));
    }

    @PreDestroy
    public void destory() throws IOException {
        restClient.close();
    }

    @Override
    public EsDoumentCursor search(String index, BSONObject queryBody) throws FullTextException {
        return new EsDoumentCursorImpl(restClient, index, queryBody, esConfig);
    }

    @Override
    public void deleteAsyncByDocId(String index, String docid) {
        DeleteRequest deleteRequest = new DeleteRequest(index, "_doc", docid);
        restClient.deleteAsync(deleteRequest, new DeleteDocAsyncListener(index, docid));
    }

    @Override
    public void deleteAsyncByFileId(String index, String fileId) {
        try {
            String filter = String.format("{ \"query\": {\"term\": {\"%s\": \"%s\"} }  }",
                    FulltextDocDefine.FIELD_FILE_ID, fileId);
            HttpEntity entity = new NStringEntity(filter, ContentType.APPLICATION_JSON);
            Map<String, String> params = Collections.singletonMap("conflicts", "proceed");
            restClient.getLowLevelClient().performRequestAsync("POST",
                    String.format("/%s/_doc/_delete_by_query", index), params, entity,
                    new DeleteDocByFileIdRespListener(index, fileId));
        }
        catch (Exception e) {
            logger.warn("failed to delete index document:index={}, fileId={}", index, fileId, e);
        }
    }

    @Override
    public void refreshIndexSilence(String index) {
        RefreshRequest refreshRequest = new RefreshRequest(index);
        try {
            RefreshResponse resp = restClient.indices().refresh(refreshRequest);
            logger.debug(
                    "refresh index resp:index={}, FailedShards={}, SuccessfulShards={}, TotalShards={}, RestStatus={}",
                    index, resp.getFailedShards(), resp.getSuccessfulShards(),
                    resp.getTotalShards(), resp.getStatus());
        }
        catch (Exception e) {
            logger.warn("failed to refresh index:{}", index, e);
        }
    }

    @Override
    public String index(String index, EsDocument document, boolean syncIndex)
            throws FullTextException {
        IndexRequest req = new IndexRequest(index, "_doc");
        if (syncIndex) {
            req.setRefreshPolicy(esConfig.getSyncRefreshPolicy());
        }
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject().field(FulltextDocDefine.FIELD_FILE_ID, document.getFileId())
                    .field(FulltextDocDefine.FIELD_FILE_VERSION, document.getFileVersion())
                    .field(FulltextDocDefine.FIELD_FILE_CONTENT, document.getContent()).endObject();
            req.source(builder);
            IndexResponse resp = restClient.index(req);
            return resp.getId();
        }
        catch (ElasticsearchException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to index document in elasticsearch:index=" + index + ", document="
                            + document,
                    e);
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to index document in elasticsearch:index=" + index + ", document="
                            + document,
                    e);
        }
    }

    @Override
    public void dropIndex(String index) throws FullTextException {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(index);
        try {
            restClient.indices().delete(deleteIndexRequest);
        }
        catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                return;
            }
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to drop index in elasticsearch:index=" + index, e);
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to drop index in elasticsearch:index=" + index, e);
        }
    }

    @Override
    public void dropIndexAsync(String index) throws FullTextException {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(index);
        try {
            restClient.indices().deleteAsync(deleteIndexRequest, new DropIndexAsyncListener(index));
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to drop index in elasticsearch:index=" + index, e);
        }
    }

    @Override
    public void createIndexWithOverwrite(String index) throws FullTextException {
        CreateIndexRequest createIndexRequest = requestForCreateIndex(index);
        try {
            restClient.indices().create(createIndexRequest);
        }
        catch (ElasticsearchException e) {
            if (e.status() != RestStatus.BAD_REQUEST) {
                throw new FullTextException(ScmError.SYSTEM_ERROR,
                        "failed to create index in elasticsearch:index=" + index, e);
            }
            dropIndex(index);
            try {
                restClient.indices().create(createIndexRequest);
            }
            catch (Exception e1) {
                throw new FullTextException(ScmError.SYSTEM_ERROR,
                        "failed to create index in elasticsearch:index=" + index, e1);
            }
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to create index in elasticsearch:index=" + index, e);
        }
    }

    private CreateIndexRequest requestForCreateIndex(String index) throws FullTextException {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject()
                    .startObject("properties").startObject(FulltextDocDefine.FIELD_FILE_ID)
                    .field("type", "keyword").endObject()
                    .startObject(FulltextDocDefine.FIELD_FILE_VERSION).field("type", "keyword")
                    .endObject().startObject(FulltextDocDefine.FIELD_FILE_CONTENT)
                    .field("type", "text").field("analyzer", esConfig.getAnalyzer())
                    .field("search_analyzer", esConfig.getSearchAnalyzer()).endObject().endObject()
                    .endObject();
            XContentBuilder setting = XContentFactory.jsonBuilder().startObject()
                    .field("number_of_shards", esConfig.getIndexShards())
                    .field("number_of_replicas", esConfig.getIndexReplicas()).endObject();
            createIndexRequest.mapping("_doc", mapping);
            createIndexRequest.settings(setting);
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to build create index request:index=" + index, e);
        }
        return createIndexRequest;
    }

    public static void main(String[] args)
            throws FullTextException, InterruptedException, IOException {
        EsClientConfig config = new EsClientConfig();
        EsClientImpl esClient = new EsClientImpl(config);
        String idxName = "test-index-1";
        esClient.dropIndex(idxName);
        esClient.createIndexIfNotExist(idxName);

        EsDocument doc = new EsDocument();
        doc.setFileId("f1");
        doc.setFileVersion("1.2");
        doc.setContent("content hahaha");
        String res = esClient.index(idxName, doc, true);

        System.out.println(esClient.isIndexExist(idxName));
        esClient.refreshIndexSilence(idxName);
        EsDoumentCursor c = esClient.search(idxName, new BasicBSONObject());
        System.out.println(c.getNextBatch());
        c.close();

        esClient.deleteAsyncByDocId(idxName, res);
        logger.info("hahah");
        c = esClient.search(idxName, new BasicBSONObject());
        System.out.println(c.getNextBatch());
        c.close();

        esClient.dropIndexAsync(idxName);

        esClient.destory();
    }

    private boolean isIndexExist(String idx) {
        GetIndexRequest req = new GetIndexRequest();
        req.indices(idx);
        try {
            return restClient.indices().exists(req);
        }
        catch (Exception e) {
            logger.warn("failed to check index is exist:index={}", idx, e);
            return false;
        }
    }

    @Override
    public void createIndexIfNotExist(String index) throws FullTextException {
        CreateIndexRequest createIndexRequest = requestForCreateIndex(index);
        try {
            restClient.indices().create(createIndexRequest);
        }
        catch (ElasticsearchException e) {
            if (e.status() == RestStatus.BAD_REQUEST) {
                if (isIndexExist(index)) {
                    return;
                }
            }
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to create index in elasticsearch:index=" + index, e);
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to create index in elasticsearch:index=" + index, e);
        }

    }
}

class DropIndexAsyncListener implements ActionListener<DeleteIndexResponse> {
    private static final Logger logger = LoggerFactory.getLogger(DropIndexAsyncListener.class);

    private String indexName;

    public DropIndexAsyncListener(String indexName) {
        this.indexName = indexName;
    }

    @Override
    public void onResponse(DeleteIndexResponse response) {
        logger.debug("drop index complete in elasticsearch:index={}", indexName);
    }

    @Override
    public void onFailure(Exception e) {
        if (e instanceof ElasticsearchException) {
            ElasticsearchException esException = (ElasticsearchException) e;
            if (esException.status() == RestStatus.NOT_FOUND) {
                logger.info("index not exist, drop index complete in elasticsearch:index={}",
                        indexName, e);
                return;
            }
        }
        logger.error("failed to drop index in elasticsearch:index={}", indexName, e);
    }

}

class DeleteDocByQueryListener implements ActionListener<BulkByScrollResponse> {
    private static final Logger logger = LoggerFactory.getLogger(DeleteDocByQueryListener.class);
    private String indexName;
    private String fileId;

    public DeleteDocByQueryListener(String indexName, String fileId) {
        this.indexName = indexName;
        this.fileId = fileId;
    }

    @Override
    public void onResponse(BulkByScrollResponse response) {
        logger.debug("delete document complete in elasticsearch:index={}, fileId={}", indexName,
                fileId);
    }

    @Override
    public void onFailure(Exception e) {
        logger.error("failed to delete document in elasticsearch:index={}, fileId={}", indexName,
                fileId, e);
    }

}

class DeleteDocAsyncListener implements ActionListener<DeleteResponse> {
    private static final Logger logger = LoggerFactory.getLogger(DeleteDocAsyncListener.class);

    private String indexName;

    private String id;

    public DeleteDocAsyncListener(String indexName, String id) {
        this.indexName = indexName;
        this.id = id;
    }

    @Override
    public void onResponse(DeleteResponse response) {
        logger.debug("delete document complete in elasticsearch:index={}, id={}", indexName, id);
    }

    @Override
    public void onFailure(Exception e) {
        if (e instanceof ElasticsearchException) {
            ElasticsearchException esException = (ElasticsearchException) e;
            if (esException.status() == RestStatus.NOT_FOUND) {
                logger.debug("document notexist in elasticsearch:index={}, id={}", indexName, id);
                return;
            }
        }
        logger.error("failed to delete document in elasticsearch:index={}, id={}", indexName, id,
                e);
    }

}

class DeleteDocByFileIdRespListener implements ResponseListener {
    private static final Logger logger = LoggerFactory
            .getLogger(DeleteDocByFileIdRespListener.class);
    private String index;
    private String fileId;

    public DeleteDocByFileIdRespListener(String index, String fileId) {
        this.index = index;
        this.fileId = fileId;
    }

    @Override
    public void onFailure(Exception e) {
        logger.warn("failed to delete index document:index={}, fileId={}", index, fileId, e);
    }

    @Override
    public void onSuccess(Response arg0) {
        logger.debug("delete index document success:index={}, fileId={}", index, fileId);
    }

}