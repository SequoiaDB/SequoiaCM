package com.sequoiacm.client.element;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.util.BsonUtils;
import com.sequoiacm.common.FieldName;
import org.bson.BSONObject;

import java.util.Map;

public class ScmSftpFileLocation extends ScmContentLocation {

    private String filePath;

    public ScmSftpFileLocation(BSONObject location) throws ScmException {
        super(location);
        this.filePath = BsonUtils.getStringChecked(location,
                FieldName.ContentLocation.FIELD_FILE_PATH);
    }

    @Override
    public Map<String, Object> getFullData() {
        Map<String, Object> fullData = super.getFullData();
        fullData.put(FieldName.ContentLocation.FIELD_FILE_PATH, this.filePath);
        return fullData;
    }

    /**
     * Gets the file path.
     *
     * @return file path.
     */
    public String getFilePath() {
        return filePath;
    }
}
