package com.sequoiacm.client.element;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

import java.util.Map;

public class ScmSwiftObjLocation extends ScmContentLocation {

    private String objectId;

    private String container;

    public ScmSwiftObjLocation(BSONObject location) throws ScmException {
        super(location);
        this.container = BsonUtils.getStringChecked(location,
                FieldName.ContentLocation.FIELD_CONTAINER);
        this.objectId = BsonUtils.getStringChecked(location,
                FieldName.ContentLocation.FIELD_OBJECT_ID);
    }

    @Override
    public Map<String, Object> getFullData() {
        Map<String, Object> fullData = super.getFullData();
        fullData.put(FieldName.ContentLocation.FIELD_CONTAINER, this.container);
        fullData.put(FieldName.ContentLocation.FIELD_OBJECT_ID, this.objectId);
        return fullData;
    }

    /**
     * Gets the container name.
     *
     * @return container name.
     */
    public String getContainer() {
        return container;
    }

    /**
     * Gets the object id.
     *
     * @return object id.
     */
    public String getObjectId() {
        return objectId;
    }
}
