package com.sequoiacm.config.framework.config.workspace.metasource;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.metasource.MetaSourceDefine;
import com.sequoiacm.config.metasource.Transaction;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbMetasource;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbTableDao;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

public class SysWorkspaceHistoryTableDaoSdbImpl extends SequoiadbTableDao implements SysWorkspaceHistoryTableDao {

    public SysWorkspaceHistoryTableDaoSdbImpl(SequoiadbMetasource sdbMetasource) {
        super(sdbMetasource, MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_WORKSPACE_HISTORY);
    }

    public SysWorkspaceHistoryTableDaoSdbImpl(Transaction transaction) {
        super(transaction, MetaSourceDefine.SequoiadbTableName.CS_SCMSYSTEM,
                MetaSourceDefine.SequoiadbTableName.CL_WORKSPACE_HISTORY);
    }

    @Override
    public void initWorkspaceHistoryTable() throws MetasourceException {
        ensureCollection();

        String nameIdIndex = "idx_workspace_name_version";
        BSONObject nameIdIndexField = new BasicBSONObject();
        nameIdIndexField.put(FieldName.FIELD_CLWORKSPACE_NAME, 1);
        nameIdIndexField.put(FieldName.FIELD_CLWORKSPACE_VERSION, 1);
        ensureIndex(nameIdIndex, nameIdIndexField, true);
    }

}
