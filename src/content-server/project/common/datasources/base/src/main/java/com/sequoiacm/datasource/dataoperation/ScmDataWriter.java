package com.sequoiacm.datasource.dataoperation;

import com.sequoiacm.datasource.ScmDatasourceException;

public abstract class ScmDataWriter {
    public abstract void write(byte[] content) throws ScmDatasourceException;

    public abstract void write(byte[] content, int offset, int len) throws ScmDatasourceException;

    //remove the writing file & release writer's resources
    public abstract void cancel();

    //close and commit this file & release writer's resources
    public abstract void close() throws ScmDatasourceException;

    //update this size when writing
    public abstract long getSize();

    public final int getType() {
        return ENDataType.Normal.getValue();
    }

    // return null if did not create data table
    public abstract String getCreatedTableName();
}
