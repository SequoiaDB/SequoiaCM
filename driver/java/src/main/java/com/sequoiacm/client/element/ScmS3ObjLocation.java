package com.sequoiacm.client.element;

import java.util.Map;

import org.bson.BSONObject;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;

public class ScmS3ObjLocation extends ScmContentLocation {

    private String bucket;
    private String objectId;

    public ScmS3ObjLocation(BSONObject location) throws ScmException {
        super(location);
        this.bucket = BsonUtils.getStringChecked(location, FieldName.ContentLocation.FIELD_BUCKET);
        this.objectId = BsonUtils.getStringChecked(location,
                FieldName.ContentLocation.FIELD_OBJECT_ID);
    }

    @Override
    public Map<String, Object> getFullData() {
        Map<String, Object> fullData = super.getFullData();
        fullData.put(FieldName.ContentLocation.FIELD_BUCKET, this.bucket);
        fullData.put(FieldName.ContentLocation.FIELD_OBJECT_ID, this.objectId);
        return fullData;
    }

    /**
     * Gets the bucket name.
     *
     * @return bucket name.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Gets the object id.
     *
     * @return object id.
     */
    public String getObjectId() {
        return objectId;
    }

    @Override
    public String toString() {
        return "ScmS3ObjLocation{" + "bucket='" + bucket + '\'' + ", objectId='" + objectId + '\''
                + '}';
    }
}
