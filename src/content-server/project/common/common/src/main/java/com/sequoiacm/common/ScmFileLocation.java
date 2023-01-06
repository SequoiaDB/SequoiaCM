package com.sequoiacm.common;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.bson.BSONObject;

public class ScmFileLocation {
    // 在內部接口 deleteDataInSiteList 中使用了下列字段，如果有修改，需要考慮兼容性
    private int siteId;
    private Date date;
    private Date createDate;
    private int wsVersion;
    private String tableName;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public ScmFileLocation(BSONObject location) {
        Object temp = location.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST_ID);
        if (null != temp) {
            siteId = (Integer) temp;
        }

        temp = location.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TIME);
        if (null != temp) {
            date = new Date(CommonHelper.toLongValue(temp));
        }

        temp = location.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST_CREATE_TIME);
        if (null != temp) {
            createDate = new Date(CommonHelper.toLongValue(temp));
        }

        temp = location.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST_WS_VERSION);
        if (null != temp) {
            wsVersion = (Integer) temp;
        }
        else {
            wsVersion = 1;
        }
        temp = location.get(FieldName.FIELD_CLFILE_FILE_SITE_LIST_TABLE_NAME);
        tableName = (String) temp;
    }

    public ScmFileLocation(int siteId, long lastAccessTime, long createTime, int wsVersion,
            String tableName) {
        this.siteId = siteId;
        this.date = new Date(lastAccessTime);
        this.createDate = new Date(createTime);
        this.wsVersion = wsVersion;
        this.tableName = tableName;
    }

    public int getSiteId() {
        return siteId;
    }

    public Date getDate() {
        return date;
    }

    public Date getCreateDate() {
        return createDate;
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
        sb.append("siteId:").append(siteId).append(",");
        sb.append("lastAccessDate:").append(dateFormat.format(date)).append(",");
        sb.append("createDate:").append(dateFormat.format(createDate)).append(",");
        sb.append("wsVersion:").append(wsVersion).append(",");
        sb.append("tableName:").append(tableName);

        return sb.toString();
    }
}
