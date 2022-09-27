package com.sequoiacm.metasource.sequoiadb.accessor;

import org.bson.BasicBSONObject;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.metasource.MetaHistoryDataTableNameAccessor;
import com.sequoiacm.metasource.MetaSourceDefine;
import com.sequoiacm.metasource.sequoiadb.SdbMetaSource;
import com.sequoiacm.metasource.sequoiadb.SdbMetasourceException;

public class SdbDataTableNameHistoryAccessor extends SdbMetaAccessor implements MetaHistoryDataTableNameAccessor {
    public SdbDataTableNameHistoryAccessor(SdbMetaSource metasource) {
        super(metasource, MetaSourceDefine.CsName.CS_SCMSYSTEM,
                MetaSourceDefine.SystemClName.CL_DATA_TABLE_NAME_HISTORY, null);
    }

    @Override
    public void deleteHitoryDataTableName(String wsName, String siteName) throws SdbMetasourceException {
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.DataTableNameHistory.WORKSPACE_NAME, wsName);
        matcher.put(FieldName.DataTableNameHistory.SITE_NAME, siteName);
        matcher.put(FieldName.DataTableNameHistory.WORKSPACE_IS_DELETED, true);
        super.delete(matcher);
    }

    public void deleteHistoryDataTableRecord(String wsName, String siteName, String tableName)
            throws SdbMetasourceException {
        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(FieldName.DataTableNameHistory.WORKSPACE_NAME, wsName);
        matcher.put(FieldName.DataTableNameHistory.SITE_NAME, siteName);
        matcher.put(FieldName.DataTableNameHistory.TABLE_NAME, tableName);
        super.delete(matcher);
    }
}
