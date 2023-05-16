package com.sequoiacm.contentserver.tag.syntaxtree;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.common.module.TagName;
import com.sequoiacm.common.module.TagType;
import com.sequoiacm.infrastructure.common.ScmStringUtil;
import org.apache.commons.lang.StringUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 问号(?)：匹配任意一个字符，但不能匹配 0 个字符 "?abc" 可以匹配 "Dabc" 但不可以匹配 "abc"
 * 星号(*)：匹配任意 N 个字符，也可以匹配 0 个字符 "*abc" 可以匹配 "Dabc", "DBaabc", "abc"
 */
public abstract class TagInfoMatcher {
    private final boolean ignoreCase;
    private final TagName tagNameMatcher;

    private final boolean enabledWildcard;

    public TagInfoMatcher(TagName tagNameMatcher, boolean ignoreCase, boolean enabledWildcard)
            throws ScmSystemException {
        this.ignoreCase = ignoreCase;
        this.tagNameMatcher = tagNameMatcher;
        this.enabledWildcard = enabledWildcard;
    }

    public TagName getTagNameMatcher() {
        return tagNameMatcher;
    }

    public abstract boolean match(TagInfo tagInfo) throws ScmSystemException;

    public abstract boolean isSimpleMatcher();

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public boolean isEnabledWildcard() {
        return enabledWildcard;
    }

    // 生成查询标签库的查询子句，该子句将会被注入到如下SDB全文检索查询语句中执行标签库查询：
    // {
    // "query": {
    // "bool" : {
    // "should" : [
    // <查询子句>
    // ]
    // }
    // }
    // }
    public BSONObject genTabLibMatcher() throws ScmSystemException {
        // {tags: {term: {value: xxtag, case_insensitive: false}}}
        if (tagNameMatcher.getTagType() == TagType.TAGS) {
            BasicBSONObject ret = new BasicBSONObject();
            BasicBSONObject value = new BasicBSONObject();
            value.put("value", tagNameMatcher.getTag());
            value.put("case_insensitive", ignoreCase);
            value = new BasicBSONObject(FieldName.TagLib.TAG, value);
            if (enabledWildcard) {
                ret.put("wildcard", value);
            }
            else {
                ret.put("term", value);
            }
            return ret;
        }
        if (tagNameMatcher.getTagType() == TagType.CUSTOM_TAG) {
            BasicBSONObject ret = new BasicBSONObject();
            BasicBSONList must = new BasicBSONList();
            ret.put("bool", new BasicBSONObject("must", must));

            BasicBSONObject tagKeyTerm = new BasicBSONObject();
            BasicBSONObject tagKey = new BasicBSONObject();
            tagKey.put("value", tagNameMatcher.getTagKey());
            tagKeyTerm.put("term",
                    new BasicBSONObject(
                            FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_KEY,
                            tagKey));
            must.add(tagKeyTerm);

            if (enabledWildcard) {
                BasicBSONObject tagValueWildcard = new BasicBSONObject();
                BasicBSONObject tagValue = new BasicBSONObject();
                tagValue.put("value", tagNameMatcher.getTagValue());
                tagValue.put("case_insensitive", ignoreCase);
                tagValueWildcard.put("wildcard", new BasicBSONObject(
                        FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_VALUE,
                        tagValue));
                must.add(tagValueWildcard);
            }
            else {
                BasicBSONObject tagValueTerm = new BasicBSONObject();
                BasicBSONObject tagValue = new BasicBSONObject();
                tagValue.put("value", tagNameMatcher.getTagValue());
                tagValue.put("case_insensitive", ignoreCase);
                tagValueTerm.put("term", new BasicBSONObject(
                        FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_VALUE,
                        tagValue));
                must.add(tagValueTerm);
            }
            return ret;
        }

        throw new ScmSystemException("Unknown tag type: " + tagNameMatcher);
    }

    // 调测用
    // 生成查询标签库的查询子句，该子句将会被注入到如下SDB本地查询语句中执行标签库查询：
    // {
    // "$or": [
    // <查询子句>
    // ]
    // }
    public BSONObject genTabLibSdbLocalMatcher() throws ScmSystemException {
        BasicBSONObject ret = new BasicBSONObject();
        if (tagNameMatcher.getTagType() == TagType.TAGS) {
            if (enabledWildcard || ignoreCase) {
                BasicBSONObject regex = new BasicBSONObject();
                regex.put("$regex", genRegex(tagNameMatcher.getTag(), enabledWildcard));
                if (ignoreCase) {
                    regex.put("$options", "i");
                }
                ret.put(FieldName.TagLib.TAG, regex);
            }
            else {
                ret.put(FieldName.TagLib.TAG, tagNameMatcher.getTag());
            }
            return ret;
        }

        if (tagNameMatcher.getTagType() == TagType.CUSTOM_TAG) {
            BasicBSONObject customTag = new BasicBSONObject();
            if (enabledWildcard || ignoreCase) {
                BasicBSONObject regex = new BasicBSONObject();
                regex.put("$regex", genRegex(tagNameMatcher.getTagValue(), enabledWildcard));
                if (ignoreCase) {
                    regex.put("$options", "i");
                }
                customTag.put(tagNameMatcher.getTagType().getFileField() + "."
                        + FieldName.TagLib.CUSTOM_TAG_TAG_KEY, tagNameMatcher.getTagKey());
                customTag.put(tagNameMatcher.getTagType().getFileField() + "."
                        + FieldName.TagLib.CUSTOM_TAG_TAG_VALUE, regex);

            }
            else {
                customTag.put(
                        FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_KEY,
                        tagNameMatcher.getTagKey());
                customTag.put(
                        FieldName.TagLib.CUSTOM_TAG + "." + FieldName.TagLib.CUSTOM_TAG_TAG_VALUE,
                        tagNameMatcher.getTagValue());
            }
            return customTag;
        }
        throw new ScmSystemException("Unknown tag type: " + tagNameMatcher);
    }

    private String genRegex(String wildcard, boolean enabledWildcard) {
        if (!enabledWildcard) {
            return "^" + Pattern.quote(wildcard) + "$";
        }
        return ScmStringUtil.wildcardToRegex(wildcard);
    }
}

class TagInfoEqualsMatcher extends TagInfoMatcher {

    public TagInfoEqualsMatcher(TagName tagName) throws ScmSystemException {
        super(tagName, false, false);
    }

    @Override
    public boolean match(TagInfo tagInfo) throws ScmSystemException {
        if (tagInfo.getTagType() != getTagNameMatcher().getTagType()) {
            return false;
        }

        if (tagInfo.getTagType() == TagType.TAGS) {
            return Objects.equals(tagInfo.getTagName().getTag(), getTagNameMatcher().getTag());
        }
        if (tagInfo.getTagType() == TagType.CUSTOM_TAG) {
            return Objects.equals(tagInfo.getTagName().getTagKey(), getTagNameMatcher().getTagKey())
                    && Objects.equals(tagInfo.getTagName().getTagValue(),
                            getTagNameMatcher().getTagValue());
        }
        throw new ScmSystemException("Unknown tag type: " + tagInfo);
    }

    @Override
    public boolean isSimpleMatcher() {
        return true;
    }
}


class TagInfoEqualsIgnoreCaseMatcher extends TagInfoMatcher {

    public TagInfoEqualsIgnoreCaseMatcher(TagName tagName) throws ScmSystemException {
        super(tagName, true, false);
    }

    @Override
    public boolean match(TagInfo tagInfo) throws ScmSystemException {
        if (tagInfo.getTagType() != getTagNameMatcher().getTagType()) {
            return false;
        }

        if (tagInfo.getTagType() == TagType.TAGS) {
            return StringUtils.equalsIgnoreCase(tagInfo.getTagName().getTag(),
                    getTagNameMatcher().getTag());

        }
        if (tagInfo.getTagType() == TagType.CUSTOM_TAG) {
            return Objects.equals(tagInfo.getTagName().getTagKey(), getTagNameMatcher().getTagKey())
                    && StringUtils.equalsIgnoreCase(tagInfo.getTagName().getTagValue(),
                            getTagNameMatcher().getTagValue());
        }
        throw new ScmSystemException("Unknown tag type: " + tagInfo);
    }

    @Override
    public boolean isSimpleMatcher() {
        return true;
    }
}



class TagInfoTagsTailMatcher extends TagInfoMatcher {

    private final boolean questionMarker;
    private final String tagNoWildcard;

    public TagInfoTagsTailMatcher(TagName tagMatcher, boolean ignoreCase)
            throws ScmSystemException {
        super(tagMatcher, ignoreCase, true);
        // 问号(?)：匹配任意一个字符，但不能匹配 0 个字符
        // 星号(*)：匹配任意 N 个字符，也可以匹配 0 个字符
        questionMarker = tagMatcher.getTag().charAt(0) == '?';
        tagNoWildcard = tagMatcher.getTag().substring(1);
    }

    @Override
    public boolean match(TagInfo tagInfo) throws ScmSystemException {
        if (tagInfo.getTagType() != TagType.TAGS) {
            return false;
        }
        boolean isEndWith = false;
        if (isIgnoreCase()) {
            isEndWith = StringUtils.endsWithIgnoreCase(tagInfo.getTagName().getTag(),
                    tagNoWildcard);
        }
        else {
            isEndWith = tagInfo.getTagName().getTag().endsWith(tagNoWildcard);
        }

        if (isEndWith) {
            if (questionMarker) {
                // ?tail
                return tagInfo.getTagName().getTag().length() == tagNoWildcard.length() + 1;
            }
        }
        return isEndWith;
    }

    @Override
    public boolean isSimpleMatcher() {
        return true;
    }
}

class TagInfoCustomTagTailMatcher extends TagInfoMatcher {
    private final boolean questionMarker;
    private final String tagValueNoWildcard;

    public TagInfoCustomTagTailMatcher(TagName customTagNameMatcher, boolean ignoreCase)
            throws ScmSystemException {
        super(customTagNameMatcher, ignoreCase, true);
        // 问号(?)：匹配任意一个字符，但不能匹配 0 个字符
        // 星号(*)：匹配任意 N 个字符，也可以匹配 0 个字符
        questionMarker = customTagNameMatcher.getTagValue().charAt(0) == '?';
        tagValueNoWildcard = customTagNameMatcher.getTagValue().substring(1);
    }

    @Override
    public boolean match(TagInfo tagInfo) throws ScmSystemException {
        if (tagInfo.getTagType() != TagType.CUSTOM_TAG) {
            return false;
        }

        if (!Objects.equals(tagInfo.getTagName().getTagKey(), getTagNameMatcher().getTagKey())) {
            return false;
        }

        boolean isEndWith = false;
        if (isIgnoreCase()) {
            isEndWith = StringUtils.endsWithIgnoreCase(tagInfo.getTagName().getTagValue(),
                    tagValueNoWildcard);
        }
        else {
            isEndWith = tagInfo.getTagName().getTagValue().endsWith(tagValueNoWildcard);
        }

        if (isEndWith) {
            if (questionMarker) {
                // ?tail
                return tagInfo.getTagName().getTagValue().length() == tagValueNoWildcard.length()
                        + 1;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isSimpleMatcher() {
        return true;
    }
}

class TagInfoTagsPartMatcher extends TagInfoMatcher {

    private final boolean leftQuestionMarker;
    private final boolean rightQuestionMarker;
    private final String tagNoWildcard;

    public TagInfoTagsPartMatcher(TagName tagNameMatcher, boolean ignoreCase)
            throws ScmSystemException {
        super(tagNameMatcher, ignoreCase, true);
        // 问号(?)：匹配任意一个字符，但不能匹配 0 个字符
        // 星号(*)：匹配任意 N 个字符，也可以匹配 0 个字符
        leftQuestionMarker = tagNameMatcher.getTag().charAt(0) == '?';
        rightQuestionMarker = tagNameMatcher.getTag()
                .charAt(tagNameMatcher.getTag().length() - 1) == '?';
        tagNoWildcard = tagNameMatcher.getTag().substring(1, tagNameMatcher.getTag().length() - 1);
    }

    @Override
    public boolean match(TagInfo tagInfo) throws ScmSystemException {
        if (tagInfo.getTagType() != TagType.TAGS) {
            return false;
        }
        // 计算通配字符串在指定标签中的下标
        int idx = indexOf(tagInfo.getTagName().getTag(), tagNoWildcard, isIgnoreCase());
        if (idx == -1) {
            return false;
        }
        if (leftQuestionMarker) {
            // ?part
            if (idx != 1) {
                return false;
            }
        }

        if (rightQuestionMarker) {
            // part?
            if (idx + tagNoWildcard.length() != tagInfo.getTagName().getTag().length() - 1) {
                return false;
            }
        }
        return true;
    }

    static int indexOf(String str, String searchStr, boolean ignoreCase) {
        if (str != null && searchStr != null) {
            int len = searchStr.length();
            int max = str.length() - len;

            for (int i = 0; i <= max; ++i) {
                if (str.regionMatches(ignoreCase, i, searchStr, 0, len)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean isSimpleMatcher() {
        return true;
    }
}

class TagInfoCustomTagPartMatcher extends TagInfoMatcher {

    private final boolean leftQuestionMarker;
    private final boolean rightQuestionMarker;
    private final String customTagValueNoWildcard;

    public TagInfoCustomTagPartMatcher(TagName tagNameMatcher, boolean ignoreCase)
            throws ScmSystemException {
        super(tagNameMatcher, ignoreCase, true);
        // 问号(?)：匹配任意一个字符，但不能匹配 0 个字符
        // 星号(*)：匹配任意 N 个字符，也可以匹配 0 个字符
        leftQuestionMarker = tagNameMatcher.getTagValue().charAt(0) == '?';
        rightQuestionMarker = tagNameMatcher.getTagValue()
                .charAt(tagNameMatcher.getTagValue().length() - 1) == '?';
        customTagValueNoWildcard = tagNameMatcher.getTagValue().substring(1,
                tagNameMatcher.getTagValue().length() - 1);
    }

    @Override
    public boolean match(TagInfo tagInfo) throws ScmSystemException {
        if (tagInfo.getTagType() != TagType.CUSTOM_TAG) {
            return false;
        }
        if (!Objects.equals(tagInfo.getTagName().getTagKey(), getTagNameMatcher().getTagKey())) {
            return false;
        }

        int idx = TagInfoTagsPartMatcher.indexOf(tagInfo.getTagName().getTagValue(),
                customTagValueNoWildcard, isIgnoreCase());
        if (idx == -1) {
            return false;
        }
        if (leftQuestionMarker) {
            // ?part
            if (idx != 1) {
                return false;
            }
        }
        if (rightQuestionMarker) {
            // part?
            if (idx + customTagValueNoWildcard.length() != tagInfo.getTagName().getTagValue()
                    .length() - 1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isSimpleMatcher() {
        return true;
    }
}

class TagInfoCustomTagPrefixMatcher extends TagInfoMatcher {

    private final boolean questionMarker;
    private final String customTagValueNoWildcard;

    public TagInfoCustomTagPrefixMatcher(TagName tagNameMatcher, boolean ignoreCase)
            throws ScmSystemException {
        super(tagNameMatcher, ignoreCase, true);
        // 问号(?)：匹配任意一个字符，但不能匹配 0 个字符
        // 星号(*)：匹配任意 N 个字符，也可以匹配 0 个字符
        questionMarker = tagNameMatcher.getTagValue()
                .charAt(tagNameMatcher.getTagValue().length() - 1) == '?';
        customTagValueNoWildcard = tagNameMatcher.getTagValue().substring(0,
                tagNameMatcher.getTagValue().length() - 1);

    }

    @Override
    public boolean match(TagInfo tagInfo) throws ScmSystemException {
        if (tagInfo.getTagType() != TagType.CUSTOM_TAG) {
            return false;
        }

        if (!Objects.equals(tagInfo.getTagName().getTagKey(), getTagNameMatcher().getTagKey())) {
            return false;
        }

        if (startWith(tagInfo.getTagName().getTagValue(), customTagValueNoWildcard,
                isIgnoreCase())) {
            if (questionMarker) {
                // prefix?
                return tagInfo.getTagName().getTagValue()
                        .length() == customTagValueNoWildcard.length() + 1;
            }
            return true;
        }
        return false;
    }

    static boolean startWith(String target, String prefix, boolean ignoreCase) {
        if (ignoreCase) {
            return StringUtils.startsWithIgnoreCase(target, prefix);
        }
        return target.startsWith(prefix);
    }

    @Override
    public boolean isSimpleMatcher() {
        return true;
    }
}

class TagInfoTagsPrefixMatcher extends TagInfoMatcher {

    private final boolean questionMarker;
    private final String tagValueNoWildcard;

    public TagInfoTagsPrefixMatcher(TagName tagNameMatcher, boolean ignoreCase)
            throws ScmSystemException {
        super(tagNameMatcher, ignoreCase, true);
        // 问号(?)：匹配任意一个字符，但不能匹配 0 个字符
        // 星号(*)：匹配任意 N 个字符，也可以匹配 0 个字符
        questionMarker = tagNameMatcher.getTag()
                .charAt(tagNameMatcher.getTag().length() - 1) == '?';
        tagValueNoWildcard = tagNameMatcher.getTag().substring(0,
                tagNameMatcher.getTag().length() - 1);

    }

    @Override
    public boolean match(TagInfo tagInfo) throws ScmSystemException {
        if (tagInfo.getTagType() != TagType.TAGS) {
            return false;
        }
        if (TagInfoCustomTagPrefixMatcher.startWith(tagInfo.getTagName().getTag(),
                tagValueNoWildcard, isIgnoreCase())) {
            if (questionMarker) {
                // prefix?
                return tagInfo.getTagName().getTag().length() == tagValueNoWildcard.length() + 1;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isSimpleMatcher() {
        return true;
    }
}

// 复杂匹配器，不支持本地匹配
class ComplexTagInfoMatcher extends TagInfoMatcher {

    public ComplexTagInfoMatcher(TagName tagName, boolean ignoreCase, boolean enabledWildcard)
            throws ScmSystemException {
        super(tagName, ignoreCase, enabledWildcard);
    }

    @Override
    public boolean match(TagInfo tagInfo) {
        throw new UnsupportedOperationException("unsupported operation");
    }

    @Override
    public boolean isSimpleMatcher() {
        return false;
    }
}