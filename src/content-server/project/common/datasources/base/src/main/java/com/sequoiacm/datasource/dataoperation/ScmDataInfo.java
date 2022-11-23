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
    private int wsVersion = 1;

    public ScmDataInfo(BSONObject fileInfo, Integer wsVersion) {
        this.type = (int)fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_TYPE);
        this.id = (String)fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_ID);
        this.createTime = CommonHelper.getDate(
                (long)fileInfo.get(FieldName.FIELD_CLFILE_FILE_DATA_CREATE_TIME));
        if (wsVersion != null){
            this.wsVersion = wsVersion;
        }
    }

    public ScmDataInfo(int dataType, String dataId, Date dataCreateTime, int wsVersion) {
        this.type = dataType;
        this.id = dataId;
        this.createTime = dataCreateTime;
        this.wsVersion = wsVersion;
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

        return sb.toString();
    }

}
