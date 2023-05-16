package com.sequoiacm.contentserver.tag.syntaxtree;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.module.TagType;
import com.sequoiacm.exception.ScmServerException;

public class TagContainsMatcherNode extends TagMatcherNode {

    public TagContainsMatcherNode(TagType tagType, TagInfoMatcher tagInfoMatcher) {
        super(tagType, tagInfoMatcher);
    }

    @Override
    public BSONObject toSdbFileMatcher() throws ScmServerException {
        BSONObject ret = new BasicBSONObject();
        ret.put(tagType.getFileField(),
                new BasicBSONObject("$elemMatch", new BasicBSONObject("$in", getTagIds())));
        return ret;
    }

    @Override
    public String toString() {
        return "{" + tagType.getFileField() + ": {$contains: "
                + getTagInfoMatcher().getTagNameMatcher() + ", $enable_wildcard: "
                + getTagInfoMatcher().isEnabledWildcard() + ", $ignore_case: "
                + getTagInfoMatcher().isIgnoreCase() + "}}";
    }
}
