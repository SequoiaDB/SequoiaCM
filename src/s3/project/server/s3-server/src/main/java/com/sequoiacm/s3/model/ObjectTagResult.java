package com.sequoiacm.s3.model;

import java.util.Map;

public class ObjectTagResult {
    private String versionId;
    private Map<String, String> tagging;

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public Map<String, String> getTagging() {
        return tagging;
    }

    public void setTagging(Map<String, String> tagging) {
        this.tagging = tagging;
    }
}
