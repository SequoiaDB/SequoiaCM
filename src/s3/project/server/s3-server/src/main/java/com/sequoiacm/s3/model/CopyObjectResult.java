package com.sequoiacm.s3.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "CopyObjectResult")
public class CopyObjectResult {
    @JsonProperty("ETag")
    private String eTag;
    @JsonProperty("LastModified")
    private String lastModified;

    @JsonIgnore
    private String versionId;

    @JsonIgnore
    private String sourceVersionId;

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    public String geteTag() {
        return eTag;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getSourceVersionId() {
        return sourceVersionId;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setSourceVersionId(String sourceVersionId) {
        this.sourceVersionId = sourceVersionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }
}
