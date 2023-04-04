package com.sequoiacm.diagnose.common;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.datasource.dataoperation.ENDataType;
import com.sequoiacm.infrastructure.common.BsonUtils;
import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import java.util.Map;

public class ScmFileInfo {
    private String id;
    private String name;
    private int majorVersion;
    private int minorVersion;
    private long size;
    private String dataId;
    private BasicBSONList siteList;
    private Map<Integer, ScmFileLocation> fileLocationMap;
    private String md5;
    private boolean deleteMarker;
    private int dataType;
    private long createTime;

    public ScmFileInfo(BSONObject fileRecord) {
        this.id = BsonUtils.getString(fileRecord, FieldName.FIELD_CLFILE_ID);
        this.name = BsonUtils.getString(fileRecord, FieldName.FIELD_CLFILE_NAME);
        this.minorVersion = BsonUtils.getIntegerChecked(fileRecord,
                FieldName.FIELD_CLFILE_MINOR_VERSION);
        this.majorVersion = BsonUtils.getIntegerChecked(fileRecord,
                FieldName.FIELD_CLFILE_MAJOR_VERSION);
        Number num = BsonUtils.getNumber(fileRecord, FieldName.FIELD_CLFILE_FILE_SIZE);
        if (num != null) {
            this.size = num.longValue();
        }
        num = BsonUtils.getNumber(fileRecord, FieldName.FIELD_CLFILE_INNER_CREATE_TIME);
        if (num != null) {
            this.createTime = num.longValue();
        }
        this.dataId = BsonUtils.getString(fileRecord, FieldName.FIELD_CLFILE_FILE_DATA_ID);
        this.siteList = BsonUtils.getArray(fileRecord, FieldName.FIELD_CLFILE_FILE_SITE_LIST);
        this.md5 = BsonUtils.getString(fileRecord, FieldName.FIELD_CLFILE_FILE_MD5);
        this.fileLocationMap = CommonHelper.getFileLocationList(siteList);
        this.deleteMarker = BsonUtils.getBooleanOrElse(fileRecord,
                FieldName.FIELD_CLFILE_DELETE_MARKER, false);
        this.dataType = BsonUtils.getIntegerOrElse(fileRecord,
                FieldName.FIELD_CLFILE_FILE_DATA_TYPE, ENDataType.Normal.getValue());
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public long getSize() {
        return size;
    }

    public String getDataId() {
        return dataId;
    }

    public BasicBSONList getSiteList() {
        return siteList;
    }

    public Map<Integer, ScmFileLocation> getFileLocationMap() {
        return fileLocationMap;
    }

    public String getMd5() {
        return md5;
    }

    public ScmFileLocation getScmFileLocation(int siteId) {
        return fileLocationMap.get(siteId);
    }

    public boolean isDeleteMarker() {
        return deleteMarker;
    }

    public int getDataType() {
        return dataType;
    }

    public long getCreateTime() {
        return createTime;
    }
}
