package com.sequoiacm.contentserver.model;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;

public class DataTableNameHistoryInfo {
    private String siteName;
    private String tableName;
    private String wsName;
    private long tableCreateTime;
    private boolean wsIsDeleted;

    public DataTableNameHistoryInfo() {
    }

    public DataTableNameHistoryInfo(BSONObject record) {
        this.siteName = BsonUtils.getStringChecked(record,
                FieldName.DataTableNameHistory.SITE_NAME);
        this.tableName = BsonUtils.getStringChecked(record,
                FieldName.DataTableNameHistory.TABLE_NAME);
        this.wsName = BsonUtils.getStringChecked(record,
                FieldName.DataTableNameHistory.WORKSPACE_NAME);
        this.wsIsDeleted = BsonUtils.getBooleanChecked(record,
                FieldName.DataTableNameHistory.WORKSPACE_IS_DELETED);
        this.tableCreateTime = BsonUtils
                .getNumberOrElse(record, FieldName.DataTableNameHistory.TABLE_CREATE_TIME, 0)
                .longValue();
    }

    public boolean isWsIsDeleted() {
        return wsIsDeleted;
    }

    public void setWsIsDeleted(boolean wsIsDeleted) {
        this.wsIsDeleted = wsIsDeleted;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getWsName() {
        return wsName;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public long getTableCreateTime() {
        return tableCreateTime;
    }

    public void setTableCreateTime(long tableCreateTime) {
        this.tableCreateTime = tableCreateTime;
    }

    public BSONObject toBSONObject() {
        BSONObject rec = new BasicBSONObject();
        rec.put(FieldName.DataTableNameHistory.SITE_NAME, siteName);
        rec.put(FieldName.DataTableNameHistory.TABLE_CREATE_TIME, tableCreateTime);
        rec.put(FieldName.DataTableNameHistory.TABLE_NAME, tableName);
        rec.put(FieldName.DataTableNameHistory.WORKSPACE_IS_DELETED, wsIsDeleted);
        rec.put(FieldName.DataTableNameHistory.WORKSPACE_NAME, wsName);
        return rec;
    }
}
