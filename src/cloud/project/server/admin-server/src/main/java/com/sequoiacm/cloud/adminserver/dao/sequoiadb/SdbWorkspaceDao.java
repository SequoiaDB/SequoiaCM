package com.sequoiacm.cloud.adminserver.dao.sequoiadb;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.cloud.adminserver.common.FieldName;
import com.sequoiacm.cloud.adminserver.common.SequoiadbHelper;
import com.sequoiacm.cloud.adminserver.dao.WorkspaceDao;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.metasource.MetaAccessor;
import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;
import com.sequoiacm.cloud.adminserver.metasource.sequoiadb.SequoiadbMetaSource;
import com.sequoiacm.cloud.adminserver.model.BsonTranslator;
import com.sequoiacm.cloud.adminserver.model.WorkspaceInfo;

@Repository
public class SdbWorkspaceDao implements WorkspaceDao {

    @Autowired
    private SequoiadbMetaSource metasource;
    
    @Override
    public List<WorkspaceInfo> query() throws StatisticsException {
        List<WorkspaceInfo> wsList = new ArrayList<>();
        MetaAccessor wsAccessor = metasource.getWorkspaceAccessor();
        MetaCursor cursor = null;
        try {
            cursor = wsAccessor.query(null, null, null);
            while (cursor.hasNext()) {
                BSONObject obj = cursor.getNext();
                wsList.add(BsonTranslator.Workspace.fromBSONObject(obj));
            }
        }
        finally {
            SequoiadbHelper.closeCursor(cursor);
        }
        return wsList;
    }

    @Override
    public WorkspaceInfo queryByName(String wsName) throws StatisticsException {
        MetaAccessor wsAccessor = metasource.getWorkspaceAccessor();
        BasicBSONObject matcher = new BasicBSONObject(FieldName.Workspace.FIELD_NAME, wsName);
        BSONObject obj = SequoiadbHelper.queryOne(wsAccessor, matcher, null);
        if (obj != null) {
            return BsonTranslator.Workspace.fromBSONObject(obj);
        }
        return null;
    }
}
