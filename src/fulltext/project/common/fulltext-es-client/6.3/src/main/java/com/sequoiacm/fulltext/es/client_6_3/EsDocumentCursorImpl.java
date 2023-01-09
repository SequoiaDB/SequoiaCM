package com.sequoiacm.fulltext.es.client_6_3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sequoiacm.fulltext.es.client.base.EsClientConfig;
import com.sequoiacm.fulltext.es.client.base.EsDocumentCursor;
import com.sequoiacm.fulltext.es.client.base.EsSearchRes;
import com.sequoiacm.fulltext.es.client.base.Util;
import com.sequoiacm.fulltext.server.exception.FullTextException;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;
import org.bson.util.JSON;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.fulltext.common.FulltextDocDefine;

class EsDocumentCursorImpl implements EsDocumentCursor {
    private static final Logger logger = LoggerFactory.getLogger(EsDocumentCursorImpl.class);
    private BSONObject queryBody;
    private String index;
    private RestHighLevelClient restClient;
    private String scrollId;
    private EsClientConfig esConfig;

    EsDocumentCursorImpl(RestHighLevelClient restClient, String index, BSONObject queryBody,
                         EsClientConfig esConfig) {
        this.esConfig = esConfig;
        this.restClient = restClient;
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

        SearchScrollRequest searchScrollRequest = new SearchScrollRequest(scrollId);
        searchScrollRequest.scroll(TimeValue.timeValueMillis(esConfig.getSearchScrollTimeout()));
        SearchResponse resp;
        try {
            resp = restClient.searchScroll(searchScrollRequest);
        }
        catch (Exception e) {
            throw new FullTextException(ScmError.SYSTEM_ERROR,
                    "failed to search in elasticsearch:index=" + index + ", queryBody=" + queryBody
                            + ", scrollId=" + scrollId,
                    e);
        }
        scrollId = resp.getScrollId();
        SearchHits hits = resp.getHits();
        List<EsSearchRes> ret = new ArrayList<>();
        for (SearchHit hit : hits) {
            Map<String, Object> source = hit.getSourceAsMap();
            String fileId = (String) source.get(FulltextDocDefine.FIELD_FILE_ID);
            String version = (String) source.get(FulltextDocDefine.FIELD_FILE_VERSION);
            float score = hit.getScore();
            String docId = hit.getId();
            List<String> hightlights = new ArrayList<>();
            for (HighlightField highlight : hit.getHighlightFields().values()) {
                Text[] texts = highlight.getFragments();
                if (texts != null) {
                    for (Text text : texts) {
                        hightlights.add(text.string());
                    }
                }
            }
            ret.add(new EsSearchRes(docId, fileId, version, score, hightlights));
        }
        return ret;
    }

    private List<EsSearchRes> initScroll() throws IOException {
        HttpEntity entity = new NStringEntity(queryBody.toString(), ContentType.APPLICATION_JSON);
        Map<String, String> params = Collections.singletonMap("scroll",
                esConfig.getSearchScrollTimeout() + "ms");
        Response resp = restClient.getLowLevelClient().performRequest("POST",
                "/" + index + "/_doc/_search?scroll=" + esConfig.getSearchScrollTimeout() + "ms",
                params, entity);
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
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        try {
            restClient.clearScroll(clearScrollRequest);
        }
        catch (Exception e) {
            logger.warn("failed to clear scroll:index={},scrollId={}", index, scrollId, e);
        }
    }
}
