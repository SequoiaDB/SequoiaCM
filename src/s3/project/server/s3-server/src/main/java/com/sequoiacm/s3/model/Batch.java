package com.sequoiacm.s3.model;

import java.util.List;

public class Batch {
    private List<ListObjRecord> cmPrefix;
    private List<ListObjRecord> content;

    public Batch() {
    }

    public int getCount() {
        int c = 0;
        if (cmPrefix != null) {
            c += cmPrefix.size();
        }
        if (content != null) {
            c += content.size();
        }
        return c;
    }

    public List<ListObjRecord> getCmPrefix() {
        return cmPrefix;
    }

    public List<ListObjRecord> getContent() {
        return content;
    }

    public void setCmPrefix(List<ListObjRecord> cmPrefix) {
        this.cmPrefix = cmPrefix;
    }

    public void setContent(List<ListObjRecord> content) {
        this.content = content;
    }

}