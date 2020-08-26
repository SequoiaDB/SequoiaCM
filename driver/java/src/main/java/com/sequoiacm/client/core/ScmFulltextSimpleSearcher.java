package com.sequoiacm.client.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.dispatcher.BsonReader;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.fulltext.ScmFulltextHighlightOption;
import com.sequoiacm.client.element.fulltext.ScmFulltextSearchResult;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.client.util.BsonConverter;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.infrastructure.fulltext.common.FulltextDocDefine;
import com.sequoiacm.infrastructure.fulltext.common.FultextRestCommonDefine;

public class ScmFulltextSimpleSearcher {

    private ScmWorkspace ws;
    private ScopeType scope = ScopeType.SCOPE_CURRENT;
    private BSONObject fileCondition;
    private List<String> mustMatchs = new ArrayList<String>();
    private List<String> mustNotMatchs = new ArrayList<String>();
    private List<String> shouldMatchs = new ArrayList<String>();
    private ScmFulltextHighlightOption highlightOption;
    private double minScore;

    ScmFulltextSimpleSearcher(ScmWorkspace ws) {
        this.ws = ws;
    }

    /**
     * Set the min score.
     * @param minScore
     *          min score.
     * @return {@code this}
     */
    public ScmFulltextSimpleSearcher minScore(double minScore) {
        this.minScore = minScore;
        return this;
    }

    /**
     * Set the keyword that must match.
     * @param keywords
     *          keywords.
     * @return {@code this}
     * @throws ScmInvalidArgumentException
     *          if error happens.
     */
    public ScmFulltextSimpleSearcher match(String... keywords) throws ScmInvalidArgumentException {
        if (keywords != null && keywords.length > 0) {
            for (String keyword : keywords) {
                if (keyword == null) {
                    throw new ScmInvalidArgumentException(
                            "keywords contains null:" + Arrays.toString(keywords));
                }
                mustMatchs.add(keyword);
            }
        }
        return this;
    }

    /**
     * Set the keyword that should match.
     * @param keywords
     *          keywords.
     * @return {@code this}
     * @throws ScmInvalidArgumentException
     *          if error happens.
     */
    public ScmFulltextSimpleSearcher shouldMatch(String... keywords)
            throws ScmInvalidArgumentException {
        if (keywords != null && keywords.length > 0) {
            for (String keyword : keywords) {
                if (keyword == null) {
                    throw new ScmInvalidArgumentException(
                            "keywords contains null:" + Arrays.toString(keywords));
                }
                shouldMatchs.add(keyword);
            }
        }
        return this;
    }

    /**
     * Enable highlight and specified highlight option.
     * @param option
     *          highlight option.
     * @return {@code this}
     */
    public ScmFulltextSimpleSearcher highlight(ScmFulltextHighlightOption option) {
        this.highlightOption = option;
        return this;
    }

    /**
     * Set the file scope.
     * @param scope
     *          file scope.
     * @return {@code this}
     * @throws ScmInvalidArgumentException
     *          if error happens.
     */
    public ScmFulltextSimpleSearcher scope(ScopeType scope) throws ScmInvalidArgumentException {
        if (scope == null) {
            throw new ScmInvalidArgumentException("scop is null");
        }
        this.scope = scope;
        return this;
    }

    /**
     * Set the file condition.
     * @param fileCondition
     *          file condition.
     * @return  {@code this}
     */
    public ScmFulltextSimpleSearcher fileCondition(BSONObject fileCondition) {
        this.fileCondition = fileCondition;
        return this;
    }

    /**
     * Set the keywords that must not match.
     * @param keywords
     *          keywords.
     * @return {@code this}
     * @throws ScmInvalidArgumentException
     *          if error happens.
     */
    public ScmFulltextSimpleSearcher notMatch(String... keywords)
            throws ScmInvalidArgumentException {
        if (keywords != null && keywords.length > 0) {
            for (String keyword : keywords) {
                if (keyword == null) {
                    throw new ScmInvalidArgumentException(
                            "keywords contains null:" + Arrays.toString(keywords));
                }
                mustNotMatchs.add(keyword);
            }
        }
        return this;
    }

    private BasicBSONList matchArr(List<String> matchs) {
        BasicBSONList arr = new BasicBSONList();
        for (String match : matchs) {
            BasicBSONObject matchValue = new BasicBSONObject(FulltextDocDefine.FIELD_FILE_CONTENT,
                    match);
            arr.add(new BasicBSONObject("match", matchValue));
        }
        return arr;
    }

    /**
     * Performs search. 
     * @return A cursor to traverse
     * @throws ScmException
     *          if error happens.
     */
    public ScmCursor<ScmFulltextSearchResult> search() throws ScmException {
        BSONObject contentCondition = buildContentCondition();

        BsonReader bsonReader = ws.getSession().getDispatcher().fulltextSearch(ws.getName(),
                scope.getScope(), fileCondition == null ? new BasicBSONObject() : fileCondition,
                contentCondition);
        return new ScmBsonCursor<ScmFulltextSearchResult>(bsonReader,
                new ScmFultextSearchResBsonConverter());
    }

    private BSONObject buildContentCondition() {
        /*  此函数构建如下形式的JSON
         *  {
         *      "min_score": 0.7,
         *      "query":{
         *          "bool": {
         *              "must": [
         *                  {"match": {"fileContent": "keyword"}},
         *                  {"match": {"fileContent": "keyword"}}
         *              ],
         *              "must_not": [
         *                  {"match": {"fileContent": "keyword"}},
         *                  {"match": {"fileContent": "keyword"}}
         *              ],
         *              "should": [
         *                  {"match": {"fileContent": "keyword"}},
         *                  {"match": {"fileContent": "keyword"}}
         *              ]
         *          }
         *      },
         *      "highlight":{
         *          "pre_tags": ["<em>"],
         *          "post_tags": ["</em>"],
         *          "fields":{
         *              "fileContent":{"fragment_size":100, "number_of_fragments": 3}
         *          }
         *      }
         *  }
         */

        BSONObject contentCondition = new BasicBSONObject();
        contentCondition.put("min_score", minScore);
        BasicBSONObject queryValue = new BasicBSONObject();
        BasicBSONObject boolValue = new BasicBSONObject();
        boolValue.put("must", matchArr(mustMatchs));
        boolValue.put("must_not", matchArr(mustNotMatchs));
        boolValue.put("should", matchArr(shouldMatchs));
        queryValue.put("bool", boolValue);
        contentCondition.put("query", queryValue);
        if (highlightOption != null) {
            BSONObject highlightValue = new BasicBSONObject();
            highlightValue.put("pre_tags", Arrays.asList(highlightOption.getPreTag()));
            highlightValue.put("post_tags", Arrays.asList(highlightOption.getPostTag()));
            BSONObject fieldsValue = new BasicBSONObject();
            BasicBSONObject fileContentValue = new BasicBSONObject();
            fileContentValue.put("fragment_size", highlightOption.getFragmentSize());
            fileContentValue.put("number_of_fragments", highlightOption.getNumOfFragments());
            fieldsValue.put(FulltextDocDefine.FIELD_FILE_CONTENT, fileContentValue);
            highlightValue.put("fields", fieldsValue);
            contentCondition.put("highlight", highlightValue);
        }
        return contentCondition;
    }

}

class ScmFultextSearchResBsonConverter implements BsonConverter<ScmFulltextSearchResult> {

    @SuppressWarnings("unchecked")
    @Override
    public ScmFulltextSearchResult convert(BSONObject obj) throws ScmException {
        ScmFulltextSearchResult ret = new ScmFulltextSearchResult();
        ScmFileBasicInfo scmFileBasicInfo = new ScmFileBasicInfo(BsonUtils.getBSONObjectChecked(obj,
                FultextRestCommonDefine.FulltextSearchRes.KEY_FILE_BASIC_INFO));
        ret.setFileBasicInfo(scmFileBasicInfo);
        ret.setScore(
                BsonUtils.getNumberChecked(obj, FultextRestCommonDefine.FulltextSearchRes.KEY_SCORE)
                        .floatValue());
        ret.setHighlightTexts((List<String>) (Object) (BsonUtils.getArray(obj,
                FultextRestCommonDefine.FulltextSearchRes.KEY_HIGHLIGHT)));
        return ret;
    }

}
