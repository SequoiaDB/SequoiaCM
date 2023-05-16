package com.sequoiacm.contentserver.tag.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.contentserver.exception.ScmSystemException;
import com.sequoiacm.common.module.TagInfo;
import com.sequoiacm.common.module.TagType;

public abstract class TagMatcherNode implements TagSyntaxTreeNode {
    private final TagInfoMatcher tagInfoMatcher;
    protected TagType tagType;
    protected List<Long> tagIds = new ArrayList<>();

    public TagMatcherNode(TagType tagType, TagInfoMatcher tagInfoMatcher) {
        this.tagType = tagType;
        this.tagInfoMatcher = tagInfoMatcher;
    }

    public TagType getTagType() {
        return tagType;
    }

    public void setTagType(TagType tagType) {
        this.tagType = tagType;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }

    public void appendTagId(long tagId) {
        this.tagIds.add(tagId);
    }

    public boolean isSimpleMatcher() {
        return tagInfoMatcher.isSimpleMatcher();
    }

    public boolean isRelatedTag(TagInfo tagInfo) throws ScmSystemException {
        return tagInfoMatcher.match(tagInfo);
    }

    public TagInfoMatcher getTagInfoMatcher() {
        return tagInfoMatcher;
    }
}
