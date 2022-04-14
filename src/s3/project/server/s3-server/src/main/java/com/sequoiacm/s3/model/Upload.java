package com.sequoiacm.s3.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.s3.common.S3Codec;
import com.sequoiacm.s3.core.UploadMeta;
import com.sequoiacm.s3.exception.S3Error;
import com.sequoiacm.s3.exception.S3ServerException;
import org.bson.BSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static com.sequoiacm.s3.utils.DataFormatUtils.formatDate;

public class Upload {
    @JsonProperty("Key")
    private String key;

    @JsonProperty("UploadId")
    private long uploadId;

    @JsonProperty("Initiator")
    Owner initiator;

    @JsonProperty("Owner")
    Owner owner;

    @JsonProperty("Initiated")
    private String formatDate;

    public Upload(BSONObject record, String encodingType, Owner owner) throws S3ServerException {
        String key = record.get(UploadMeta.META_KEY_NAME).toString();
        if (key != null) {
            this.key = S3Codec.encode(key, encodingType);
        }

        if (record.get(UploadMeta.META_UPLOAD_ID) != null) {
            this.uploadId = (long) record.get(UploadMeta.META_UPLOAD_ID);
        }

        if (record.get(UploadMeta.META_LAST_MODIFY_TIME) != null) {
            this.formatDate = formatDate((long) record.get(UploadMeta.META_LAST_MODIFY_TIME));
        }

        this.owner = owner;
        this.initiator = owner;
    }

    public void setUploadId(long uploadId) {
        this.uploadId = uploadId;
    }

    public long getUploadId() {
        return uploadId;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setFormatDate(String formatDate) {
        this.formatDate = formatDate;
    }

    public String getFormatDate() {
        return formatDate;
    }

    public void setInitiator(Owner initiator) {
        this.initiator = initiator;
    }

    public Owner getInitiator() {
        return initiator;
    }
}
