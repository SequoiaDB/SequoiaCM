package com.sequoiacm.fulltext.es.client8_2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.sequoiacm.fulltext.es.client.base.EsClientConfig;
import com.sequoiacm.fulltext.es.client.base.EsDocument;
import com.sequoiacm.fulltext.es.client.base.EsDocumentCursor;
import com.sequoiacm.fulltext.es.client.base.EsSearchRes;
import com.sequoiacm.fulltext.es.client.base.Util;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.fulltext.common.FulltextDocDefine;

class EsDocumentCursorImpl implements EsDocumentCursor {
    private static final Logger logger = LoggerFactory.getLogger(EsDocumentCursorImpl.class);
    private final ElasticsearchClient esClient;
    private final BSONObject queryBody;
    private final String index;
    private final RestClient restClient;
    private String scrollId;
    private final EsClientConfig esConfig;

    EsDocumentCursorImpl(RestClient restClient, ElasticsearchClient esClient, String index,
            BSONObject queryBody, EsClientConfig esConfig) {
        this.esConfig = esConfig;
        this.restClient = restClient;
        this.esClient = esClient;
        this.index = index;
        this.queryBody = queryBody;
        this.queryBody.put("_source", Arrays.asList(FulltextDocDefine.FIELD_FILE_ID,
                FulltextDocDefine.FIELD_FILE_VERSION));
        if (!queryBody.containsField("size")) {
            this.queryBody.put("size", esConfig.getSearchScrollSize());
        }
    }

    @Override
    public List<EsSearchRes> getNextBatch() throws FullTextException {
        if (scrollId == null) {
            try {
                return initScroll();
            }
            catch (Exception e) {
                throw new FullTextException(ScmError.SYSTEM_ERROR,
                        "failed to search in elasticsearch:index=" + index + ", queryBody="
                                + queryBody,
                        e);
            }
        }
        try {
            ScrollResponse<EsDocument> resp = esClient.scroll(r -> r.scrollId(scrollId),
                    EsDocument.class);
            HitsMetadata<EsDocument> hits = resp.hits();
            List<EsSearchRes> ret = new ArrayList<>();
            for (Hit<EsDocument> hit : hits.hits()) {
                float score = hit.score() == null ? 0 : hit.score().floatValue();
                String docId = hit.id();
                List<String> highlights = new ArrayList<>();
                for (List<String> highlight : hit.highlight().values()) {
                    highlights.addAll(highlight);
                }
                ret.add(new EsSearchRes(docId, hit.source().getFileId(),
                        hit.source().getFileVersion(), score, highlights));
            }
            return ret;
        }
        catch (IOException e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR, "failed to do scroll", e);
        }
    }

    private List<EsSearchRes> initScroll() throws IOException {
        HttpEntity entity = new NStringEntity(queryBody.toString(), ContentType.APPLICATION_JSON);
        Map<String, String> params = Collections.singletonMap("scroll",
                esConfig.getSearchScrollTimeout() + "ms");
        Request req = new Request("POST",
                "/" + index + "/_search?scroll=" + esConfig.getSearchScrollTimeout() + "ms");
        req.addParameters(params);
        req.setEntity(entity);
        Response resp = restClient.performRequest(req);
        BSONObject bson = (BSONObject) JSON.parse(EntityUtils.toString(resp.getEntity()));
        scrollId = BsonUtils.getStringChecked(bson, "_scroll_id");
        BSONObject hits = BsonUtils.getBSON(bson, "hits");
        if (hits == null) {
            return Collections.emptyList();
        }

        BasicBSONList hitsArr = BsonUtils.getArray(hits, "hits");
        return Util.parseEsScrollHits(hitsArr);
    }

    @Override
    public void close() {
        if (scrollId == null) {
            return;
        }
        try {
            esClient.clearScroll(r -> r.scrollId(scrollId));
        }
        catch (Exception e) {
            logger.warn("failed to clear scroll:index={},scrollId={}", index, scrollId, e);
        }
    }
}
