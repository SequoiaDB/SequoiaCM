package com.sequoiacm.client.element;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

import java.util.Map;

public class ScmHdfsFileLocation extends ScmContentLocation {

    private String fileName;

    private String directory;

    public ScmHdfsFileLocation(BSONObject location) throws ScmException {
        super(location);
        this.fileName = BsonUtils.getStringChecked(location,
                FieldName.ContentLocation.FIELD_FILE_NAME);
        this.directory = BsonUtils.getStringChecked(location,
                FieldName.ContentLocation.FIELD_DIRECTORY);
    }

    @Override
    public Map<String, Object> getFullData() {
        Map<String, Object> fullData = super.getFullData();
        fullData.put(FieldName.ContentLocation.FIELD_FILE_NAME, this.fileName);
        fullData.put(FieldName.ContentLocation.FIELD_DIRECTORY, this.directory);
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
     * Gets the directory.
     *
     * @return directory.
     */
    public String getDirectory() {
        return directory;
    }
}
