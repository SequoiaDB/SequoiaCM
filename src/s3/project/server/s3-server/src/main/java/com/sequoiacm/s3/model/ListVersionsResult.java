package com.sequoiacm.s3.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.sequoiacm.s3.common.S3Codec;
import com.sequoiacm.s3.exception.S3ServerException;

@JacksonXmlRootElement(localName = "ListVersionsResult")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ListVersionsResult {
    @JsonProperty("Name")
    private String name;

    @JsonProperty("Prefix")
    private String prefix;

    @JsonProperty("Delimiter")
    private String delimiter;

    @JsonProperty("KeyMarker")
    private String keyMarker;

    @JsonProperty("VersionIdMarker")
    private String versionIdMarker;

    @JsonProperty("MaxKeys")
    private int maxKeys;

    @JsonProperty("EncodingType")
    private String encodingType;

    @JsonProperty("IsTruncated")
    private Boolean isTruncated = false;

    @JsonProperty("NextKeyMarker")
    private String nextKeyMarker;

    @JsonProperty("NextVersionIdMarker")
    private String nextVersionIdMarker;

    @JacksonXmlElementWrapper(localName = "Version", useWrapping = false)
    @JsonProperty("Version")
    private List<ListObjVersion> versionList = new ArrayList<>();

    @JacksonXmlElementWrapper(localName = "DeleteMarker", useWrapping = false)
    @JsonProperty("DeleteMarker")
    private LinkedHashSet<RawVersion> deleteMarkerList = new LinkedHashSet<>();

    @JacksonXmlElementWrapper(localName = "CommonPrefixes", useWrapping = false)
    @JsonProperty("CommonPrefixes")
    private List<ListObjectCommonPrefix> commonPrefixList = new ArrayList<>();

    public ListVersionsResult(String bucketName, Integer maxKeys, String encodingType,
            String prefix, String delimiter, String keyMarker, String versionIdMarker)
            throws S3ServerException {
        this.name = bucketName;
        this.maxKeys = maxKeys;
        this.encodingType = encodingType;
        this.versionIdMarker = versionIdMarker;
        this.prefix = S3Codec.encode(prefix, encodingType);
        this.delimiter = S3Codec.encode(delimiter, encodingType);
        this.keyMarker = S3Codec.encode(keyMarker, encodingType);
    }

    public void setCommonPrefixList(List<ListObjectCommonPrefix> commonPrefixList) {
        if (commonPrefixList != null) {
            this.commonPrefixList = commonPrefixList;
        }
    }

    public List<ListObjectCommonPrefix> getCommonPrefixList() {
        return commonPrefixList;
    }

    public void setDeleteMarkerList(LinkedHashSet<RawVersion> deleteMarkerList) {
        if (deleteMarkerList != null) {
            this.deleteMarkerList = deleteMarkerList;
        }
    }

    public LinkedHashSet<RawVersion> getDeleteMarkerList() {
        return deleteMarkerList;
    }

    public void setVersionList(List<ListObjVersion> versionList) {
        if (versionList != null) {
            this.versionList = versionList;
        }
    }

    public List<ListObjVersion> getVersionList() {
        return versionList;
    }

    public void setIsTruncated(Boolean isTruncated) {
        this.isTruncated = isTruncated;
    }

    public Boolean getIsTruncated() {
        return isTruncated;
    }

    public void setNextKeyMarker(String nextKeyMarker) {
        this.nextKeyMarker = nextKeyMarker;
    }

    public String getNextKeyMarker() {
        return nextKeyMarker;
    }

    public void setNextVersionIdMarker(String nextVersionIdMarker) {
        this.nextVersionIdMarker = nextVersionIdMarker;
    }

    public String getNextVersionIdMarker() {
        return nextVersionIdMarker;
    }

    public void addVersion(ListObjVersion content) {
        versionList.add(content);
    }

    public void addCommonPrefix(ListObjectCommonPrefix prefix) {
        commonPrefixList.add(prefix);
    }
}
