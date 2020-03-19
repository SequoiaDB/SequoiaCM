package com.sequoiacm.s3.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ListObjRecord {
    // content
    @JsonProperty("Key")
    private String key;
    @JsonProperty("LastModified")
    private String lastModified;
    @JsonProperty("ETag")
    private String eTag;
    @JsonProperty("Size")
    private long size;
    @JsonProperty("Owner")
    private Owner owner;

    // commonPrefix
    @JsonProperty("Prefix")
    private String prefix;

    @JsonIgnore
    private boolean isContent;

    public ListObjRecord(String key, String lastModified, String eTag, long size, Owner owner) {
        this.key = key;
        this.lastModified = lastModified;
        this.eTag = eTag;
        this.size = size;
        this.owner = owner;
        isContent = true;
    }

    public ListObjRecord(String prefix) {
        this.prefix = prefix;
        isContent = false;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    public String geteTag() {
        return eTag;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public Owner getOwner() {
        return owner;
    }

    public boolean isContent() {
        return isContent;
    }
}
