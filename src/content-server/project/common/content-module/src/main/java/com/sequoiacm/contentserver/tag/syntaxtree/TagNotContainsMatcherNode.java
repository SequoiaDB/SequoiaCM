package com.sequoiacm.contentserver.tag.syntaxtree;

import com.sequoiacm.common.module.TagType;
import com.sequoiacm.exception.ScmServerException;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class TagNotContainsMatcherNode extends TagMatcherNode {

    public TagNotContainsMatcherNode(TagType tagType, TagInfoMatcher tagInfoMatcher) {
        super(tagType, tagInfoMatcher);
    }

    @Override
    public BSONObject toSdbFileMatcher() throws ScmServerException {
        BSONObject ret = new BasicBSONObject();
        ret.put(tagType.getFileField(), new BasicBSONObject("$nin", getTagIds()));
        return ret;
    }

    @Override
    public String toString() {
        return "{" + tagType.getFileField() + ": {$not_contains: "
                + getTagInfoMatcher().getTagNameMatcher() + ", $enable_wildcard: "
                + getTagInfoMatcher().isEnabledWildcard() + ", $ignore_case: "
                + getTagInfoMatcher().isIgnoreCase() + "}}";
    }
}
