package com.sequoiacm.client.element;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

import java.util.Map;

public class ScmHbaseFileLocation extends ScmContentLocation {

    private String fileName;

    private String tableName;

    public ScmHbaseFileLocation(BSONObject location) throws ScmException {
        super(location);
        this.fileName = BsonUtils.getStringChecked(location,
                FieldName.ContentLocation.FIELD_FILE_NAME);
        this.tableName = BsonUtils.getStringChecked(location,
                FieldName.ContentLocation.FIELD_TABLE_NAME);
    }

    @Override
    public Map<String, Object> getFullData() {
        Map<String, Object> fullData = super.getFullData();
        fullData.put(FieldName.ContentLocation.FIELD_FILE_NAME, this.fileName);
        fullData.put(FieldName.ContentLocation.FIELD_TABLE_NAME, this.tableName);
        return fullData;
    }

    /**
     * Gets the file name.
     *
     * @return file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets the table name.
     *
     * @return table name.
     */
    public String getTableName() {
        return tableName;
    }

}
