package com.sequoiacm.contentserver.tag.syntaxtree;

import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.common.module.TagName;
import com.sequoiacm.common.module.TagType;
import org.bson.BSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;


@Component
public class TagInfoMatcherBuilder {

    public TagInfoMatcher build(TagType tagType, Object matcher, boolean enabledValueWildcard,
            boolean valueIgnoreCase) throws ScmInvalidArgumentException, ScmSystemException {
        TagName tagNameMatcher = createTagNameMatcher(tagType, matcher);

        if (tagType == TagType.TAGS) {
            String matcherString = tagNameMatcher.getTag();
            matcherString = formatMatcherString(matcherString, enabledValueWildcard);
            tagNameMatcher.setTag(matcherString);
            MatcherType matcherType = getMatcherType(matcherString, enabledValueWildcard);
            return createTagsInfoMatcher(matcherType, tagNameMatcher, enabledValueWildcard,
                    valueIgnoreCase);
        }
        if (tagType == TagType.CUSTOM_TAG) {
            String matcherString = tagNameMatcher.getTagValue();
            matcherString = formatMatcherString(matcherString, enabledValueWildcard);
            tagNameMatcher.setTagValue(matcherString);
            MatcherType matcherType = getMatcherType(matcherString, enabledValueWildcard);
            return createCustomTagInfoMatcher(matcherType, tagNameMatcher, enabledValueWildcard,
                    valueIgnoreCase);
        }

        throw new ScmSystemException("Unknown tag type: " + tagType);
    }

    private TagInfoMatcher createCustomTagInfoMatcher(MatcherType matcherType,
            TagName tagNameMatcher, boolean enabledValueWildcard, boolean valueIgnoreCase)
            throws ScmSystemException {
        switch (matcherType) {
            case PREFIX:
                return new TagInfoCustomTagPrefixMatcher(tagNameMatcher, valueIgnoreCase);
            case TAIL:
                return new TagInfoCustomTagTailMatcher(tagNameMatcher, valueIgnoreCase);
            case PART:
                return new TagInfoCustomTagPartMatcher(tagNameMatcher, valueIgnoreCase);
            case EQUALS:
                return valueIgnoreCase ? new TagInfoEqualsIgnoreCaseMatcher(tagNameMatcher)
                        : new TagInfoEqualsMatcher(tagNameMatcher);
            case COMPLEX:
                return new ComplexTagInfoMatcher(tagNameMatcher, valueIgnoreCase,
                        enabledValueWildcard);
            default:
                throw new ScmSystemException("unknown matcher type: " + matcherType);
        }
    }

    private TagInfoMatcher createTagsInfoMatcher(MatcherType matcherType, TagName tagNameMatcher,
            boolean enabledValueWildcard, boolean valueIgnoreCase) throws ScmSystemException {
        switch (matcherType) {
            case PREFIX:
                return new TagInfoTagsPrefixMatcher(tagNameMatcher, valueIgnoreCase);
            case TAIL:
                return new TagInfoTagsTailMatcher(tagNameMatcher, valueIgnoreCase);
            case PART:
                return new TagInfoTagsPartMatcher(tagNameMatcher, valueIgnoreCase);
            case EQUALS:
                return valueIgnoreCase ? new TagInfoEqualsIgnoreCaseMatcher(tagNameMatcher)
                        : new TagInfoEqualsMatcher(tagNameMatcher);
            case COMPLEX:
                return new ComplexTagInfoMatcher(tagNameMatcher, valueIgnoreCase,
                        enabledValueWildcard);
            default:
                throw new ScmSystemException("unknown matcher type: " + matcherType);
        }
    }

    private static String formatMatcherString(String matcherString, boolean enableWildcard) {
        if (!enableWildcard) {
            return matcherString;
        }

        // 去除重复的 *
        StringBuilder ret = new StringBuilder();
        boolean escape = false;
        boolean lastCharIsMatchAny = false;
        for (int i = 0; i < matcherString.length(); i++) {
            char c = matcherString.charAt(i);
            if (lastCharIsMatchAny && c == '*') {
                continue;
            }
            ret.append(c);
            if (!escape && c == '*') {
                lastCharIsMatchAny = true;
                continue;
            }
            lastCharIsMatchAny = false;

            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
            }
        }

        return ret.toString();
    }

    private MatcherType getMatcherType(String matcherString, boolean enableWildcard) {
        if (!enableWildcard) {
            return MatcherType.EQUALS;
        }
        ArrayList<Integer> wildcardIdx = new ArrayList<>();
        boolean escape = false;
        for (int i = 0; i < matcherString.length(); i++) {
            if (escape) {
                escape = false;
                continue;
            }
            if (matcherString.charAt(i) == '\\') {
                escape = true;
                continue;
            }
            if (matcherString.charAt(i) == '?' || matcherString.charAt(i) == '*') {
                wildcardIdx.add(i);
            }
        }

        // 没有通配符
        if (wildcardIdx.isEmpty()) {
            return MatcherType.EQUALS;
        }

        if (wildcardIdx.size() == 1) {
            // 只有一个通配符，并且是开头位置
            if (wildcardIdx.get(0) == 0) {
                return MatcherType.TAIL;
            }
            // 只有一个通配符，并且是结尾位置
            if (wildcardIdx.get(0) == matcherString.length() - 1) {
                return MatcherType.PREFIX;
            }
            return MatcherType.COMPLEX;
        }

        if (wildcardIdx.size() == 2) {
            // 只有两个通配符，并且是开头位置
            if (wildcardIdx.get(0) == 0 && wildcardIdx.get(1) == matcherString.length() - 1) {
                return MatcherType.PART;
            }
            return MatcherType.COMPLEX;
        }

        return MatcherType.COMPLEX;
    }

    private enum MatcherType {
        EQUALS,
        PREFIX,
        TAIL,
        PART,
        COMPLEX
    }

    private TagName createTagNameMatcher(TagType tagType, Object matcher)
            throws ScmInvalidArgumentException, ScmSystemException {
        if (tagType == TagType.TAGS) {
            if (!(matcher instanceof String)) {
                throw new ScmInvalidArgumentException("tags matcher must be a string: " + matcher);
            }
            return TagName.tags((String) matcher);
        }

        if (tagType == TagType.CUSTOM_TAG) {
            if (!(matcher instanceof BSONObject)) {
                throw new ScmInvalidArgumentException(
                        "custom_tag matcher must be a key value pair: " + matcher);
            }

            BSONObject matcherBSON = (BSONObject) matcher;
            if (matcherBSON.keySet().size() != 1) {
                throw new ScmInvalidArgumentException(
                        "custom_tag matcher must be a key value pair: " + matcher);
            }

            String key = matcherBSON.keySet().iterator().next();
            Object valueObj = matcherBSON.get(key);
            if (!(valueObj instanceof String)) {
                throw new ScmInvalidArgumentException(
                        "custom_tag matcher must be a key value pair: " + matcher);
            }
            String value = (String) valueObj;

            return TagName.customTag(key, value);
        }

        throw new ScmSystemException("Unknown tag type: " + tagType);
    }
}