package com.sequoiacm.mq.server.dao;

public class TableCreateResult {
    private String tableName;
    private boolean isAlreadyExist;

    public String getTableName() {
        return tableName;
    }

    public boolean isAlreadyExist() {
        return isAlreadyExist;
    }

    public void setAlreadyExist(boolean isAlreadyExist) {
        this.isAlreadyExist = isAlreadyExist;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
