package com.sequoiacm.tools.tag.common;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class FileBasicInfo {
    public static final String STATUS_FAILED_FILE_SCOPE = "file_scope";
    private String fileId;
    private int majorVersion;

    private int minorVersion;

    private FileScope scope;

    public FileBasicInfo(String fileId, int majorVersion, int minorVersion, FileScope scope) {
        this.fileId = fileId;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.scope = scope;
    }

    public FileBasicInfo(FileScope scope, BSONObject record) {
        this.scope = scope;
        this.fileId = BsonUtils.getStringChecked(record, FieldName.FIELD_CLFILE_ID);
        this.majorVersion = BsonUtils.getNumberChecked(record, FieldName.FIELD_CLFILE_MAJOR_VERSION)
                .intValue();
        this.minorVersion = BsonUtils.getNumberChecked(record, FieldName.FIELD_CLFILE_MINOR_VERSION)
                .intValue();
    }

    public FileBasicInfo(BSONObject failedFileBsonInStatusFile) {
        this.fileId = BsonUtils.getStringChecked(failedFileBsonInStatusFile,
                FieldName.FIELD_CLFILE_ID);
        this.majorVersion = BsonUtils
                .getNumberChecked(failedFileBsonInStatusFile, FieldName.FIELD_CLFILE_MAJOR_VERSION)
                .intValue();
        this.minorVersion = BsonUtils
                .getNumberChecked(failedFileBsonInStatusFile, FieldName.FIELD_CLFILE_MINOR_VERSION)
                .intValue();
        this.scope = FileScope
                .valueOf(BsonUtils.getStringChecked(failedFileBsonInStatusFile, "file_scope"));
    }

    @Override
    public String toString() {
        return "{" + "fileId='" + fileId + '\'' + ", majorVersion=" + majorVersion
                + ", minorVersion=" + minorVersion + ", scope=" + scope + '}';
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public FileScope getScope() {
        return scope;
    }

    public void setScope(FileScope scope) {
        this.scope = scope;
    }

    public BSONObject asStatusFailedFileBson() {
        BSONObject bson = new BasicBSONObject();
        bson.put(FieldName.FIELD_CLFILE_ID, fileId);
        bson.put(FieldName.FIELD_CLFILE_MAJOR_VERSION, majorVersion);
        bson.put(FieldName.FIELD_CLFILE_MINOR_VERSION, minorVersion);
        bson.put(STATUS_FAILED_FILE_SCOPE, scope.name());
        return bson;
    }
}
