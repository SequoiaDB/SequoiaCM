package com.sequoiacm.datasource.dataoperation;

import java.util.Date;

import org.bson.BSONObject;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;

public class ScmDataInfo {
    // no used yet!
    private int type;
    private String id;
    private Date createTime;
    private int wsVersion = 1;
    private String tableName;

    // forCreateNewData: 创建的 dataInfo 传递给底层数据源，用于创建新的数据
    public static ScmDataInfo forCreateNewData(BSONObject fileInfo, Integer wsVersion) {
        ScmDataInfo ret = new ScmDataInfo();
        ret.type = (int) fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_TYPE);
        ret.id = (String) fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        ret.createTime = CommonHelper
                .getDate((long) fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME));
        if (wsVersion != null) {
            ret.wsVersion = wsVersion;
        }
        return ret;
    }

    public static ScmDataInfo forCreateNewData(int dataType, String dataId, Date dataCreateTime,
            int wsVersion) {
        ScmDataInfo ret = new ScmDataInfo();
        ret.type = dataType;
        ret.id = dataId;
        ret.createTime = dataCreateTime;
        ret.wsVersion = wsVersion;
        return ret;
    }

    // forOpenExistData: 创建的 dataInfo 传递给底层数据源，用于打开已存在的数据
    public static ScmDataInfo forOpenExistData(BSONObject fileInfo, Integer wsVersion,
            String tableName) {
        ScmDataInfo ret = new ScmDataInfo();
        ret.type = (int) fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_TYPE);
        ret.id = (String) fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        ret.createTime = CommonHelper
                .getDate((long) fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME));
        if (wsVersion != null) {
            ret.wsVersion = wsVersion;
        }
        ret.tableName = tableName;
        return ret;
    }

    public static ScmDataInfo forOpenExistData(int dataType, String dataId, Date dataCreateTime,
            int wsVersion, String tableName) {
        ScmDataInfo ret = new ScmDataInfo();
        ret.type = dataType;
        ret.id = dataId;
        ret.createTime = dataCreateTime;
        ret.wsVersion = wsVersion;
        ret.tableName = tableName;
        return ret;
    }

    private ScmDataInfo() {
    }

    public int getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setWsVersion(int wsVersion) {
        this.wsVersion = wsVersion;
    }

    public int getWsVersion() {
        return wsVersion;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("dataType=");
        sb.append(type);
        sb.append(",dataId=");
        sb.append(id);
        sb.append(",createTime=");
        sb.append(createTime);
        sb.append(",wsVersion=");
        sb.append(wsVersion);
        sb.append(",tableName=");
        sb.append(tableName);

        return sb.toString();
    }

}
