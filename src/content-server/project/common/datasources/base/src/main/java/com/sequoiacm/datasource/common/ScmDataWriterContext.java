package com.sequoiacm.datasource.common;

/**
 * scm data writer context
 */
public class ScmDataWriterContext {

    private String tableName;

    public ScmDataWriterContext() {
    }

    public ScmDataWriterContext(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public void recordTableName(String tableName) {
        this.tableName = tableName;
    }
}
