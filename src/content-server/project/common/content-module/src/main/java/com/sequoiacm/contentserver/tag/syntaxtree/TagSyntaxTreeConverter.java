package com.sequoiacm.contentserver.tag.syntaxtree;

import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.common.module.TagName;
import com.sequoiacm.contentserver.exception.ScmOperationUnsupportedException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.tag.*;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TagSyntaxTreeConverter {
    private static final Logger logger = LoggerFactory.getLogger(TagSyntaxTreeConverter.class);

    @Value("${scm.tag.condition.tagCount:10000}")
    private int tagIdMaxCount = 10000;

    @Autowired
    private TagLibMgr tagLibMgr;

    public BSONObject convertToSdbFileMatcher(ScmWorkspaceInfo ws, TagSyntaxTree tree)
            throws ScmServerException {
        TagIdInjector tagIdInjector = new TagIdInjectorImpl(tagLibMgr, ws, tagIdMaxCount,
                tree.getMatcherNodes());
        tagIdInjector.inject();
        return tree.getRoot().toSdbFileMatcher();
    }
}

abstract class TagIdInjector {
    private static final Logger logger = LoggerFactory.getLogger(TagIdInjector.class);
    private TagLibMgr tagLibMgr;
    private ScmWorkspaceInfo ws;

    private final List<TagMatcherNode> matcherNodes;
    private List<TagMatcherNode> simpleMatcher = new ArrayList<>();
    private List<TagName> exactTagNameList = new ArrayList<>();

    private List<TagMatcherNode> complexMatcher = new ArrayList<>();
    private int tagIdCount;
    private int maxTagIdCount;

    public TagIdInjector(TagLibMgr tagLibMgr, ScmWorkspaceInfo ws, int maxTagIdCount,
            List<TagMatcherNode> matcherNodes) {
        this.tagLibMgr = tagLibMgr;
        this.ws = ws;
        this.maxTagIdCount = maxTagIdCount;
        this.matcherNodes = matcherNodes;
    }

    public void inject() throws ScmServerException {
        for (TagMatcherNode matcherNode : matcherNodes) {
            if (matcherNode.isSimpleMatcher()) {
                simpleMatcher.add(matcherNode);
                TagInfoMatcher matcher = matcherNode.getTagInfoMatcher();

                // 如果是精确匹配（大小写敏感），额外登记
                // 后续若发现所有简单匹配都是精确匹配，则走SDB本地索引精确查询
                if (matcher instanceof TagInfoEqualsMatcher) {
                    TagInfoEqualsMatcher tagsEqualsMatcher = (TagInfoEqualsMatcher) matcher;
                    exactTagNameList.add(tagsEqualsMatcher.getTagNameMatcher());
                }
                continue;
            }

            // 后续需要为每个复杂匹配节点单独查询一次标签库
            complexMatcher.add(matcherNode);
        }
        processSimpleMatcherNode();
        processComplexMatcherNode();
    }

    private void appendTagId(TagInfo tagInfo, TagMatcherNode matcherNode)
            throws ScmOperationUnsupportedException {
        if (tagIdCount >= maxTagIdCount) {
            throw new ScmOperationUnsupportedException("Too many tags in one query");
        }
        matcherNode.appendTagId(tagInfo.getTagId());
        tagIdCount++;

    }

    private void processComplexMatcherNode() throws ScmServerException {
        // 每个复杂匹配节点单独查询一次标签库
        for (TagMatcherNode matcherNode : complexMatcher) {
            BSONObject condition = genComplexMatcherNodeCondition(matcherNode);
            logger.debug("complex matcher node: node={}, tagLibMatcher={}", matcherNode, condition);
            TagInfoCursor cursor = tagLibMgr.queryTag(ws, condition);
            try {
                while (cursor.hasNext()) {
                    TagInfo tagInfo = cursor.getNext();
                    logger.debug("tag lib query result: {}", tagInfo);
                    appendTagId(tagInfo, matcherNode);
                }
            }
            finally {
                cursor.close();
            }
        }
    }

    private void processSimpleMatcherNode() throws ScmServerException {
        if (exactTagNameList.size() == simpleMatcher.size()) {
            // 所有的匹配节点都是精确匹配，直接SDB本地精确查询
            logger.debug("all matcher node is exact tag matcher: {}", exactTagNameList);
            List<TagInfo> tagInfoList = tagLibMgr.getTagInfo(ws, exactTagNameList);
            // 将查询结果分配给各个匹配节点
            for (TagInfo tagInfo : tagInfoList) {
                logger.debug("tag lib query result: {}", tagInfo);
                for (TagMatcherNode matcherNode : simpleMatcher) {
                    if (matcherNode.isRelatedTag(tagInfo)) {
                        appendTagId(tagInfo, matcherNode);
                    }
                }
            }
        }
        else {
            // 所有简单匹配节点生成一个统一的 SDB 全文检索匹配条件
            BSONObject tagLibMatcher = genSimpleMatcherNodeCondition(simpleMatcher);
            logger.debug("all matcher node is simple tag matcher, gen one tag lib matcher: {}",
                    tagLibMatcher);
            TagInfoCursor tagInfoCursor = tagLibMgr.queryTag(ws, tagLibMatcher);
            try {
                // 将查询结果分配给各个匹配节点
                while (tagInfoCursor.hasNext()) {
                    TagInfo tagInfo = tagInfoCursor.getNext();
                    logger.debug("tag lib query result: {}", tagInfo);
                    for (TagMatcherNode matcherNode : simpleMatcher) {
                        if (matcherNode.isRelatedTag(tagInfo)) {
                            appendTagId(tagInfo, matcherNode);
                        }
                    }
                }
            }
            finally {
                tagInfoCursor.close();
            }
        }
    }

    protected abstract BSONObject genSimpleMatcherNodeCondition(
            List<TagMatcherNode> simpleMatcherNodes) throws ScmSystemException;

    protected abstract BSONObject genComplexMatcherNodeCondition(TagMatcherNode matcherNode)
            throws ScmServerException;
}

class TagIdInjectorImpl extends TagIdInjector {

    public TagIdInjectorImpl(TagLibMgr tagLibMgr, ScmWorkspaceInfo ws, int maxTagIdCount,
            List<TagMatcherNode> matcherNodes) {
        super(tagLibMgr, ws, maxTagIdCount, matcherNodes);
    }

    @Override
    protected BSONObject genSimpleMatcherNodeCondition(List<TagMatcherNode> simpleMatcherNodes)
            throws ScmSystemException {
        BSONObject fulltextBson = new BasicBSONObject();
        BasicBSONList fulltextSubCondition = new BasicBSONList();
        for (TagMatcherNode matcherNode : simpleMatcherNodes) {
            TagInfoMatcher matcher = matcherNode.getTagInfoMatcher();
            fulltextSubCondition.add(matcher.genTabLibMatcher());
        }
        fulltextBson.put("query",
                new BasicBSONObject("bool", new BasicBSONObject("should", fulltextSubCondition)));

        // { "": { "$Text": <fulltextBSON> } }
        return new BasicBSONObject("", new BasicBSONObject("$Text", fulltextBson));
    }

    @Override
    protected BSONObject genComplexMatcherNodeCondition(TagMatcherNode matcherNode)
            throws ScmSystemException {
        BSONObject subMatcher = matcherNode.getTagInfoMatcher().genTabLibMatcher();

        // { "": { "$Text": <fulltextBSON> } }
        return new BasicBSONObject("",
                new BasicBSONObject("$Text", new BasicBSONObject("query", subMatcher)));
    }
}
//
// class TagIdInjectorSdbLocalQueryImpl extends TagIdInjector {
//
// public TagIdInjectorSdbLocalQueryImpl(TagLibMgr tagLibMgr, ScmWorkspaceInfo
// ws,
// int maxTagIdCount, List<TagMatcherNode> matcherNodes) {
// super(tagLibMgr, ws, maxTagIdCount, matcherNodes);
// }
//
// @Override
// protected BSONObject genSimpleMatcherNodeCondition(List<TagMatcherNode>
// simpleMatcherNodes)
// throws ScmSystemException {
// BSONObject sdbLocalQueryBson = new BasicBSONObject();
// BasicBSONList dollarOrCondition = new BasicBSONList();
// for (TagMatcherNode matcherNode : simpleMatcherNodes) {
// TagInfoMatcher matcher = matcherNode.getTagInfoMatcher();
// dollarOrCondition.add(matcher.genTabLibSdbLocalMatcher());
// }
// sdbLocalQueryBson.put("$or", dollarOrCondition);
// return sdbLocalQueryBson;
// }
//
// @Override
// protected BSONObject genComplexMatcherNodeCondition(TagMatcherNode
// matcherNode)
// throws ScmSystemException {
// return matcherNode.getTagInfoMatcher().genTabLibSdbLocalMatcher();
// }
// }
