package com.sequoiacm.s3.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.sequoiacm.s3.core.Bucket;

@JacksonXmlRootElement(localName = "ListAllMyBucketsResult")
@JsonPropertyOrder({ "owner", "buckets" })
public class ListBucketResult {

    @JacksonXmlElementWrapper()
    @JsonProperty("Owner")
    private Owner owner;

    @JacksonXmlElementWrapper(localName = "Buckets")
    @JsonProperty("Bucket")
    private List<Bucket> buckets;

    public void setBuckets(List<Bucket> buckets) {
        this.buckets = buckets;
    }

    public List<Bucket> getBuckets() {
        return buckets;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }
}
