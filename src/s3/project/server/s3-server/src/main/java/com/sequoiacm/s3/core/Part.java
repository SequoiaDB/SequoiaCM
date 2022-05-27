package com.sequoiacm.s3.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import org.bson.BSONObject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.sequoiacm.s3.utils.DataFormatUtils;

public class Part {
    public static final String UPLOADID = "upload_id";
    public static final String PARTNUMBER = "part_number";
    public static final String DATAID = "data_id";
    public static final String DATA_CREATE_TIME = "data_create_time";
    public static final String SIZE = "size";
    public static final String ETAG = "etag";
    public static final String LASTMODIFIED = "last_modified";

    @JsonIgnore
    private Long uploadId;
    @JsonProperty("PartNumber")
    private int partNumber;
    @JsonProperty("ETag")
    private String etag;
    @JsonIgnore
    private Long lastModified;
    @JsonProperty("LastModified")
    private String lastModifiedDate;
    @JsonProperty("Size")
    private long size;
    @JsonIgnore
    private String dataId;
    @JsonIgnore
    private Long dataCreateTime;

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public long getSize() {
        return size;
    }

    public String getEtag() {
        return etag;
    }

    public Part() {
    }

    public Part(BSONObject record) {
        if (record.get(Part.UPLOADID) != null) {
            this.uploadId = (long) record.get(Part.UPLOADID);
        }
        if (record.get(Part.PARTNUMBER) != null) {
            this.partNumber = (int) record.get(Part.PARTNUMBER);
        }
        if (record.get(Part.DATAID) != null) {
            this.dataId = record.get(Part.DATAID).toString();
        }
        if (record.get(Part.LASTMODIFIED) != null) {
            this.lastModified = (long) record.get(Part.LASTMODIFIED);
            // this.lastModifiedDate = formatDate(this.lastModified);
        }
        if (record.get(Part.ETAG) != null) {
            this.etag = record.get(Part.ETAG).toString();
        }
        if (record.get(Part.SIZE) != null) {
            this.size = (long) record.get(Part.SIZE);
        }
        if (record.get(Part.DATA_CREATE_TIME) != null) {
            this.dataCreateTime = (long) record.get(Part.DATA_CREATE_TIME);
        }
    }

    public Part(BSONObject record, String encodingType) throws S3ServerException {
        try {
            if (record.get(Part.UPLOADID) != null) {
                this.uploadId = (long) record.get(Part.UPLOADID);
            }
            if (record.get(Part.PARTNUMBER) != null) {
                this.partNumber = (int) record.get(Part.PARTNUMBER);
            }
            if (record.get(Part.LASTMODIFIED) != null) {
                this.lastModified = (long) record.get(Part.LASTMODIFIED);
                this.lastModifiedDate = DataFormatUtils.formatISO8601Date(this.lastModified);
            }
            if (encodingType != null) {
                this.etag = URLEncoder.encode("\"" + record.get(Part.ETAG).toString() + "\"",
                        "UTF-8");
            }
            else {
                this.etag = "\"" + record.get(Part.ETAG).toString() + "\"";
            }
            if (record.get(Part.SIZE) != null) {
                this.size = (long) record.get(Part.SIZE);
            }
            if (record.get(Part.DATAID) != null) {
                this.dataId = record.get(Part.DATAID).toString();
            }
            if (record.get(Part.DATA_CREATE_TIME) != null) {
                this.dataCreateTime = (long) record.get(Part.DATA_CREATE_TIME);
            }
        }
        catch (UnsupportedEncodingException e) {
            throw new S3ServerException(S3Error.INTERNAL_ERROR, "URL encode failed", e);
        }
    }

    public Part(long uploadId, int partNumber, String dataId, Long dataCreateTime, long size,
            String eTag) {
        this.uploadId = uploadId;
        this.partNumber = partNumber;
        this.size = size;
        this.etag = eTag;
        this.lastModified = System.currentTimeMillis();
        this.dataId = dataId;
        this.dataCreateTime = dataCreateTime;
    }

    public void setUploadId(Long uploadId) {
        this.uploadId = uploadId;
    }

    public Long getUploadId() {
        return uploadId;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataCreateTime(long dataCreateTime) {
        this.dataCreateTime = dataCreateTime;
    }

    public long getDataCreateTime() {
        return dataCreateTime;
    }
}
