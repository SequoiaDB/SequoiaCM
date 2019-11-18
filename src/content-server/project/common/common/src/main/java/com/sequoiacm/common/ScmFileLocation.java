package com.sequoiacm.common;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.bson.BSONObject;

public class ScmFileLocation {
    private int siteId;
    private Date date;
    private Date createDate;
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
    }

    public ScmFileLocation(int siteId, long lastAccessTime) {
        this.siteId = siteId;
        this.date = new Date(lastAccessTime);
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("siteId:").append(siteId).append(",");
        sb.append("lastAccessDate:").append(dateFormat.format(date)).append(",");
        sb.append("createDate:").append(dateFormat.format(createDate));

        return sb.toString();
    }
}
