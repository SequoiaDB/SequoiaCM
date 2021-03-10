package com.sequoiacm.s3.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.sequoiacm.s3.common.S3Codec;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;

@JacksonXmlRootElement(localName = "ListBucketResult")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ListObjectsResult {
    @JsonProperty("Name")
    private String name;

    @JsonProperty("Prefix")
    private String prefix;

    @JsonProperty("StartAfter")
    private String startAfter;

    @JsonProperty("KeyCount")
    private int keyCount = 0;

    @JsonProperty("MaxKeys")
    private int maxKeys;

    @JsonProperty("Delimiter")
    private String delimiter;

    @JsonProperty("IsTruncated")
    private Boolean isTruncated = false;

    @JsonProperty("ContinuationToken")
    private String continueToken;

    @JsonProperty("NextContinuationToken")
    private String nextContinueToken;

    @JsonProperty("EncodingType")
    private String encodingType;

    @JacksonXmlElementWrapper(localName = "Contents", useWrapping = false)
    @JsonProperty("Contents")
    private List<ListObjRecord> contentList = new ArrayList<>();

    @JacksonXmlElementWrapper(localName = "CommonPrefixes", useWrapping = false)
    @JsonProperty("CommonPrefixes")
    private List<ListObjRecord> commonPrefixList = new ArrayList<>();

    public ListObjectsResult(String bucketName, Integer maxKeys, String encodingType, String prefix,
            String startAfter, String delimiter, String continueToken) throws S3ServerException {
        this.name = bucketName;
        this.maxKeys = maxKeys;
        this.keyCount = 0;
        this.encodingType = encodingType;
        this.prefix = S3Codec.encode(prefix, encodingType);
        this.delimiter = S3Codec.encode(delimiter, encodingType);
        this.startAfter = S3Codec.encode(startAfter, encodingType);
        this.continueToken = S3Codec.encode(continueToken, encodingType);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setStartAfter(String startAfter) {
        this.startAfter = startAfter;
    }

    public String getStartAfter() {
        return startAfter;
    }

    public void setMaxKeys(int maxKeys) {
        this.maxKeys = maxKeys;
    }

    public int getMaxKeys() {
        return maxKeys;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setIsTruncated(Boolean isTruncated) {
        this.isTruncated = isTruncated;
    }

    public Boolean getIsTruncated() {
        return isTruncated;
    }

    public void setKeyCount(int keyCount) {
        this.keyCount = keyCount;
    }

    public int getKeyCount() {
        return keyCount;
    }

    public void setContinueToken(String continueToken) {
        this.continueToken = continueToken;
    }

    public String getContinueToken() {
        return continueToken;
    }

    public void setNextContinueToken(String nextContinueToken) {
        this.nextContinueToken = nextContinueToken;
    }

    public String getNextContinueToken() {
        return nextContinueToken;
    }

    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    public String getEncodingType() {
        return encodingType;
    }

    public void setContentList(List<ListObjRecord> contentList) {
        if (contentList != null) {
            this.contentList = contentList;
        }
    }

    public List<ListObjRecord> getContentList() {
        return contentList;
    }

    public void setCommonPrefixList(List<ListObjRecord> commonPrefixList) {
        if (commonPrefixList != null) {
            this.commonPrefixList = commonPrefixList;
        }
    }

    public List<ListObjRecord> getCommonPrefixList() {
        return commonPrefixList;
    }
}
