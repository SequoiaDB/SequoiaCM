package com.sequoiacm.datasource.dataoperation;

import java.util.Date;

import org.bson.BSONObject;

import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.FieldName;

public class ScmDataInfo {
    //no used yet!
    private int type;
    private String id;
    private Date createTime;

    public ScmDataInfo(BSONObject fileInfo) {
        type = (int)fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_TYPE);
        id = (String)fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        createTime = CommonHelper.getDate(
                (long)fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME));
    }

    public ScmDataInfo(int dataType, String dataId, Date dataCreateTime) {
        type = dataType;
        id = dataId;
        createTime = dataCreateTime;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("dataType=");
        sb.append(type);
        sb.append(",dataId=");
        sb.append(id);
        sb.append(",createTime=");
        sb.append(createTime);

        return sb.toString();
    }

}
