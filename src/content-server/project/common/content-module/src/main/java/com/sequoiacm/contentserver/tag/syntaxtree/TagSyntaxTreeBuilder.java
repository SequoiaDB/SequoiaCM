package com.sequoiacm.contentserver.tag.syntaxtree;

import com.google.common.collect.Sets;
import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.common.module.TagType;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class TagSyntaxTreeBuilder {
    @Autowired
    private List<TagSyntaxTreeNodeFactory> factories;

    public TagSyntaxTree buildSyntaxTree(BSONObject bson)
            throws ScmInvalidArgumentException, ScmSystemException {
        TagSyntaxTreeNode root = buildSyntaxTreeNode(bson);
        return new TagSyntaxTree(root);
    }

    // 每个节点只能有一个 key，如 {tags: {xx}}、{$and: [xx]}
    public TagSyntaxTreeNode buildSyntaxTreeNode(BSONObject treeBson)
            throws ScmInvalidArgumentException, ScmSystemException {
        for (TagSyntaxTreeNodeFactory factory : factories) {
            TagSyntaxTreeNode node = factory.create(treeBson, this);
            if (node != null) {
                return node;
            }
        }
        throw new ScmInvalidArgumentException("Invalid tag syntax, unrecognized:" + treeBson);
    }
}

interface TagSyntaxTreeNodeFactory {

    // 返回 null 表示这个 factory 不识别这个 bson
    // 抛异常表示这个 factory 识别这个 bson，但是语法有问题
    // 当这个 bson 包含子节点，子节点 BSON 须交由 TagSyntaxTreeBuilder builder 参数继续处理
    TagSyntaxTreeNode create(BSONObject bson, TagSyntaxTreeBuilder builder)
            throws ScmInvalidArgumentException, ScmSystemException;
}

abstract class BoolNodeBasicFactory implements TagSyntaxTreeNodeFactory {

    private final String boolKey;

    // boolKey: $and、$or
    public BoolNodeBasicFactory(String boolKey) {
        this.boolKey = boolKey;
    }

    @Override
    public TagSyntaxTreeNode create(BSONObject bson, TagSyntaxTreeBuilder builder)
            throws ScmInvalidArgumentException, ScmSystemException {
        Object boolArr = bson.get(boolKey);
        if (boolArr == null) {
            return null;
        }

        if (bson.keySet().size() != 1) {
            throw new ScmInvalidArgumentException("Invalid " + boolKey + " syntax, " + boolKey
                    + " should be the only key: " + bson);
        }
        if (!(boolArr instanceof List)) {
            throw new ScmInvalidArgumentException(
                    "Invalid " + boolKey + " syntax, " + boolKey + " should be an array: " + bson);
        }

        List<Object> boolList = (List<Object>) boolArr;
        List<TagSyntaxTreeNode> subNodes = new ArrayList<>();
        for (Object subBson : boolList) {
            if (!(subBson instanceof BSONObject)) {
                throw new ScmInvalidArgumentException("Invalid " + boolKey + " syntax, " + boolKey
                        + " should be an array of BSONObject: " + bson);
            }

            TagSyntaxTreeNode subNode = builder.buildSyntaxTreeNode((BSONObject) subBson);
            subNodes.add(subNode);
        }
        return create(subNodes);
    }

    abstract TagBoolTreeNode create(List<TagSyntaxTreeNode> subNodes);
}

@Component
class OrBoolNodeFactory extends BoolNodeBasicFactory {
    public OrBoolNodeFactory() {
        super("$or");
    }

    @Override
    TagBoolTreeNode create(List<TagSyntaxTreeNode> subNodes) {
        return new TagOrBoolNode(subNodes);
    }
}

@Component
class AndBoolNodeFactory extends BoolNodeBasicFactory {
    public AndBoolNodeFactory() {
        super("$and");
    }

    @Override
    TagBoolTreeNode create(List<TagSyntaxTreeNode> subNodes) {
        return new TagAndBoolNode(subNodes);
    }
}

abstract class ContainsNodeFactoryBase implements TagSyntaxTreeNodeFactory {

    @Autowired
    TagInfoMatcherBuilder simpleMatcherBuilder;

    private static String OP_ENABLE_WILDCARD = "$enable_wildcard";
    private static String OP_IGNORE_CASE = "$ignore_case";
    private static Set<String> AVAILABLE_OPTIONS = Sets.newHashSet(OP_ENABLE_WILDCARD,
            OP_IGNORE_CASE);

    // $contains 、$not_contains
    abstract String getMatcherKey();

    // bson like:
    // {"tags": {"$not_contains": "tag1", "$enable_wildcard": true, "$ignore_case":
    // true}}
    // {"custom_tag": {"$contains": {"tagKey": "tagValue"}, "$enable_wildcard":
    // true, "$ignore_case": true}}
    @Override
    public TagSyntaxTreeNode create(BSONObject bson, TagSyntaxTreeBuilder builder)
            throws ScmInvalidArgumentException, ScmSystemException {
        Set<String> keySet = bson.keySet();

        // 先找键 "tags"、"custom_tag" ，没有的话说明不是 $contains 、$not_contains 节点
        TagType tagType = null;
        for (String key : keySet) {
            tagType = TagType.fromFileField(key);
            if (tagType != null) {
                break;
            }
        }
        if (tagType == null) {
            return null;
        }
        if (keySet.size() != 1) {
            throw new ScmInvalidArgumentException(
                    "Invalid syntax, only support one key for bson: " + bson);
        }

        // 取出 tags/custom_tag 键的值 {"$contains": "tag1", "$enable_wildcard": true,
        // "$ignore_case": true}
        // 类型不是 BSON 的话说明不是 $contains 、$not_contains 节点
        Object matcher = bson.get(tagType.getFileField());
        if (!(matcher instanceof BSONObject)) {
            return null;
        }

        // matcher = {"$contains/$not_contains": "tag1", "$enable_wildcard": true,
        // "$ignore_case": true}
        // 这里确认 matcher 包含 $contains/$not_contains 键，采用 containsField() 判断
        // get(key) 无法准确判断 matcher 是否包含指定 key，因为 matcher 可能是 {key: null}
        BSONObject matcherBson = (BSONObject) matcher;
        if (!matcherBson.containsField(getMatcherKey())) {
            // 不是 $contains/$not_contains 节点
            return null;
        }
        Object matcherObj = matcherBson.get(getMatcherKey());
        if (matcherObj == null) {
            throw new ScmInvalidArgumentException(
                    "Invalid syntax, " + getMatcherKey() + " should not be null: " + bson);
        }

        // 确认 matcher 中只包含识别的键
        for (String key : matcherBson.keySet()) {
            if (!AVAILABLE_OPTIONS.contains(key) && !key.equals(getMatcherKey())) {
                throw new ScmInvalidArgumentException(
                        "Invalid " + getMatcherKey() + " syntax, invalid option: " + key);
            }
        }

        // 识别参数 $enable_wildcard、$ignore_case
        Object enabledWildcard = BsonUtils.getObjectOrElse(matcherBson, OP_ENABLE_WILDCARD, false);
        if (!(enabledWildcard instanceof Boolean)) {
            throw new ScmInvalidArgumentException(
                    "Invalid $not_contains syntax, $enable_wildcard should be a boolean: " + bson);
        }
        Object ignoreCase = BsonUtils.getObjectOrElse(matcherBson, OP_IGNORE_CASE, false);
        if (!(ignoreCase instanceof Boolean)) {
            throw new ScmInvalidArgumentException(
                    "Invalid $not_contains syntax, $ignore_case should be a boolean: " + bson);
        }

        TagInfoMatcher tagInfoMatcher = simpleMatcherBuilder.build(tagType, matcherObj,
                (Boolean) enabledWildcard, (Boolean) ignoreCase);
        return createNode(tagType, tagInfoMatcher);
    }

    protected abstract TagSyntaxTreeNode createNode(TagType tagType, TagInfoMatcher tagInfoMatcher);
}

@Component
class TagNotContainsNodeFactory extends ContainsNodeFactoryBase {
    @Override
    String getMatcherKey() {
        return "$not_contains";
    }

    @Override
    protected TagSyntaxTreeNode createNode(TagType tagType, TagInfoMatcher tagInfoMatcher) {
        return new TagNotContainsMatcherNode(tagType, tagInfoMatcher);
    }
}

@Component
class TagContainsNodeFactory extends ContainsNodeFactoryBase {
    @Override
    String getMatcherKey() {
        return "$contains";
    }

    @Override
    protected TagSyntaxTreeNode createNode(TagType tagType, TagInfoMatcher tagInfoMatcher) {
        return new TagContainsMatcherNode(tagType, tagInfoMatcher);
    }
}
