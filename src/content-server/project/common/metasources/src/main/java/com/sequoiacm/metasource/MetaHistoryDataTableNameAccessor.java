package com.sequoiacm.metasource;

import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;

public interface MetaHistoryDataTableNameAccessor extends MetaAccessor {

    public void deleteHitoryDataTableName(String wsName, String siteName) throws SdbMetasourceException;

    public void deleteHistoryDataTableRecord(String wsName, String siteName, String tableName)
            throws SdbMetasourceException;
}
