package com.sequoiacm.client.element;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

import java.util.Map;

public class ScmSdbLobLocation extends ScmContentLocation {

    private String cs;
    private String cl;
    private String lobId;

    public ScmSdbLobLocation(BSONObject location) throws ScmException {
        super(location);
        this.cs = BsonUtils.getStringChecked(location, FieldName.ContentLocation.FIELD_CS);
        this.cl = BsonUtils.getStringChecked(location, FieldName.ContentLocation.FIELD_CL);
        this.lobId = BsonUtils.getStringChecked(location, FieldName.ContentLocation.FIELD_LOB_ID);
    }

    @Override
    public Map<String, Object> getFullData() {
        Map<String, Object> fullData = super.getFullData();
        fullData.put(FieldName.ContentLocation.FIELD_CS, this.cs);
        fullData.put(FieldName.ContentLocation.FIELD_CL, this.cl);
        fullData.put(FieldName.ContentLocation.FIELD_LOB_ID, this.lobId);
        return fullData;
    }

    /**
     * Gets the collection space.
     *
     * @return collection space name.
     */
    public String getCs() {
        return cs;
    }

    /**
     * Gets the collection.
     *
     * @return collection name.
     */
    public String getCl() {
        return cl;
    }

    /**
     * Gets the lob id.
     *
     * @return lob id.
     */
    public String getLobId() {
        return lobId;
    }
}
