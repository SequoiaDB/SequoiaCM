package com.sequoiacm.s3.model;

import java.util.Map;
import java.util.TreeMap;

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
        // 使用 tree map 对外保证有序
        this.tagging = new TreeMap<>(tagging);
    }
}
