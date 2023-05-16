package com.sequoiacm.om.omserver.module.tag;

public abstract class OmTagBasic {

    private long id;
    private OmTagType tagType;
    protected String tagContent;

    public OmTagBasic(long id, OmTagType tagType) {
        this.id = id;
        this.tagType = tagType;
    }

    public long getId() {
        return id;
    }

    public String getType() {
        return tagType.getType();
    }

    public abstract String getTagContent();
}
