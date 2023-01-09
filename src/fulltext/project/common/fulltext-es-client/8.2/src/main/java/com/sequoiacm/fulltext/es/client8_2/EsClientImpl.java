package com.sequoiacm.fulltext.es.client8_2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.RefreshResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.util.ObjectBuilder;
import com.sequoiacm.fulltext.es.client.base.EsClient;
import com.sequoiacm.fulltext.es.client.base.EsClientConfig;
import com.sequoiacm.fulltext.es.client.base.EsDocument;
import com.sequoiacm.fulltext.es.client.base.EsDocumentCursor;
import com.sequoiacm.fulltext.es.client.base.SyncRefreshPolicy;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.bson.BSONObject;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.fulltext.common.FulltextDocDefine;

public class EsClientImpl implements EsClient {
    public static final String INDEX_NOT_FOUND_EXCEPTION = "index_not_found_exception";
    private static final Logger logger = LoggerFactory.getLogger(EsClientImpl.class);
    public static final String RESOURCE_ALREADY_EXISTS_EXCEPTION = "resource_already_exists_exception";
    private final RestClient lowLevelRestClient;
    private final ElasticsearchClient restClient;
    private final ElasticsearchAsyncClient asyncRestClient;
    private final RestClientTransport transport;

    private final EsClientConfig esConfig;

    public EsClientImpl(EsClientConfig esConfig) throws Exception {
        this.esConfig = esConfig;
        List<HttpHost> httpHosts = new ArrayList<>();
        for (String url : esConfig.getUrls()) {
            URL u = new URL(url);
            httpHosts.add(new HttpHost(u.getHost(), u.getPort(), u.getProtocol()));
        }

        final BasicCredentialsProvider cred = new BasicCredentialsProvider();
        if (esConfig.getUser() != null && esConfig.getPassword() != null) {
            cred.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(esConfig.getUser(), esConfig.getPassword()));
        }
        SSLContext context = null;
        if (esConfig.getCertPath() != null) {
            Path p = Paths.get(esConfig.getCertPath());
            CertificateFactory caFactory = CertificateFactory.getInstance("X.509");
            Certificate ca;
            try (InputStream is = Files.newInputStream(p)) {
                ca = caFactory.generateCertificate(is);
            }

            KeyStore truststore = KeyStore.getInstance("pkcs12");
            truststore.load(null, null);
            truststore.setCertificateEntry("ca", ca);
            SSLContextBuilder contextBuilder = SSLContexts.custom().loadTrustMaterial(truststore,
                    null);
            context = contextBuilder.build();
        }
        final SSLContext finalContext = context;
        try {
            lowLevelRestClient = RestClient
                    .builder(httpHosts.toArray(new HttpHost[0]))
                    .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                            .setSSLContext(finalContext).setDefaultCredentialsProvider(cred))
                    .build();
            transport = new RestClientTransport(lowLevelRestClient, new JacksonJsonpMapper());
            restClient = new ElasticsearchClient(transport);
            asyncRestClient = new ElasticsearchAsyncClient(transport);
            String version = restClient.info().version().number();
            if (!version.startsWith("8.2")) {
                logger.warn(
                        "elasticsearch server version mismatch sequoiacm elasticsearch adapter: expect 8.2.x but {}",
                        version);
            }
            else {
                logger.info("elasticsearch server version {}", version);
            }
        }
        catch (Exception e) {
            destroy();
            throw e;
        }
    }

    public void destroy() {
        try {
            if (asyncRestClient != null) {
                asyncRestClient.shutdown();
            }
        }
        catch (Exception e) {
            logger.warn("failed to shutdown async rest client", e);
        }

        try {
            if (restClient != null) {
                restClient.shutdown();
            }
        }
        catch (Exception e) {
            logger.warn("failed to shutdown rest client", e);
        }


        try {
            if (transport != null) {
                transport.close();
            }
        }
        catch (Exception e) {
            logger.warn("failed to close rest client transport", e);
        }

        try {
            if (lowLevelRestClient != null) {
                lowLevelRestClient.close();
            }
        }
        catch (Exception e) {
            logger.warn("failed to close low level rest client", e);
        }
    }

    @Override
    public EsDocumentCursor search(String index, BSONObject queryBody) {
        return new EsDocumentCursorImpl(lowLevelRestClient, restClient, index, queryBody, esConfig);
    }

    @Override
    public void deleteAsyncByDocId(String index, String docId) {
        asyncRestClient.delete(t -> t.id(docId).index(index)).whenComplete((resp, exception) -> {
            if (exception != null) {
                logger.error("async delete es doc failed: index={}, docId={}", index, docId,
                        exception);
            }
            else {
                logger.debug("delete es doc complete: index={}, docId={}", index, docId);
            }
        });
    }

    @Override
    public void deleteAsyncByFileId(String index, String fileId) {
        asyncRestClient
                .deleteByQuery(t -> t.index(index).query(
                        q -> q.term(m -> m.field(FulltextDocDefine.FIELD_FILE_ID).value(fileId))))
                .whenComplete((resp, exception) -> {
                    if (exception != null) {
                        logger.error("async delete es doc failed: index={}, fileId={}", index,
                                fileId, exception);
                    }
                    else {
                        logger.debug("delete es doc complete: index={}, fileId={}", index, fileId);
                    }
                });
    }

    @Override
    public void refreshIndexSilence(String index) {
        try {
            RefreshResponse resp = restClient.indices().refresh(q -> q.index(index));
            logger.debug(
                    "refresh index resp:index={}, FailedShards={}, SuccessfulShards={}, TotalShards={}",
                    index, resp.shards().failed(), resp.shards().successful(),
                    resp.shards().total());
        }
        catch (Exception e) {
            logger.warn("failed to refresh index:{}", index, e);
        }
    }

    @Override
    public String index(String index, EsDocument document, boolean syncIndex)
            throws FullTextException {
        try {
            Refresh refresh;
            if (!syncIndex) {
                refresh = Refresh.False;
            }
            else {
                refresh = esConfig.getSyncRefreshPolicy() == SyncRefreshPolicy.WAIT_UNTIL
                        ? Refresh.WaitFor
                        : Refresh.True;
            }
            IndexResponse resp = restClient
                    .index(t -> t.index(index).refresh(refresh).document(document));
            return resp.id();
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
        try {
            restClient.indices().delete(t -> t.index(index));
        }
        catch (ElasticsearchException e) {
            if (Objects.equals(e.response().error().type(), INDEX_NOT_FOUND_EXCEPTION)) {
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
    public void dropIndexAsync(String index) {
        asyncRestClient.indices().delete(t -> t.index(index)).whenComplete((resp, exception) -> {
            if (exception != null) {
                if (exception instanceof ElasticsearchException) {
                    if (Objects.equals(
                            ((ElasticsearchException) exception).response().error().type(),
                            INDEX_NOT_FOUND_EXCEPTION)) {
                        logger.debug("drop index complete(index not found): index={}", index);
                        return;
                    }
                }
                logger.error("failed to drop index: index={}", index, exception);
            }
            else {
                logger.debug("drop index complete: index={}", index);
            }
        });
    }

    @Override
    public void createIndexWithOverwrite(String index) throws FullTextException {
        Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>> indexOption = getIndexBuilderFunc(
                index);
        try {
            restClient.indices().create(indexOption);
        }
        catch (ElasticsearchException e) {
            if (Objects.equals(e.response().error().type(), RESOURCE_ALREADY_EXISTS_EXCEPTION)) {
                dropIndex(index);
                try {
                    restClient.indices().create(indexOption);
                    return;
                }
                catch (Exception ex) {
                    throw new FullTextException(ScmError.SYSTEM_ERROR, "failed to create index",
                            ex);
                }
            }
            throw new FullTextException(ScmError.SYSTEM_ERROR, "failed to create index", e);
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR, "failed to create index", e);
        }
    }

    private Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>> getIndexBuilderFunc(
            String index) {
        return c -> c.index(index)
                .mappings(m -> m.properties(FulltextDocDefine.FIELD_FILE_ID, p -> p.keyword(k -> k))
                        .properties(FulltextDocDefine.FIELD_FILE_VERSION, p -> p.keyword(k -> k))
                        .properties(FulltextDocDefine.FIELD_FILE_CONTENT,
                                p -> p.text(t -> t.searchAnalyzer(esConfig.getSearchAnalyzer())
                                        .analyzer(esConfig.getAnalyzer()))))
                .settings(s -> s.numberOfShards(esConfig.getIndexShards() + "")
                        .numberOfReplicas(esConfig.getIndexReplicas() + ""));
    }

    @Override
    public void createIndexIfNotExist(String index) throws FullTextException {
        try {
            restClient.indices().create(getIndexBuilderFunc(index));
        }
        catch (ElasticsearchException e) {
            if (e.response().error().type().equals(RESOURCE_ALREADY_EXISTS_EXCEPTION)) {
                return;
            }
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to create index: index=" + index, e);
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to create index: index=" + index, e);
        }
    }
}
