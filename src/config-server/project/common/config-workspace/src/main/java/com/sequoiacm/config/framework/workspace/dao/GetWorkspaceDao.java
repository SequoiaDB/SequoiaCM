package com.sequoiacm.config.framework.workspace.dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.workspace.metasource.SysWorkspaceTableDao;
import com.sequoiacm.config.framework.workspace.metasource.WorkspaceMetaSerivce;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.msg.BsonConverterMgr;
import com.sequoiacm.infrastructure.config.core.msg.Config;
import com.sequoiacm.infrastructure.config.core.msg.workspace.WorkspaceFilter;

@Component
public class GetWorkspaceDao {

    @Autowired
    private BsonConverterMgr bsonConverterMgr;
    @Autowired
    private WorkspaceMetaSerivce workspaceMetaservice;

    public List<Config> getWorkspace(WorkspaceFilter filter) throws ScmConfigException {
        SysWorkspaceTableDao wsMetaTable = workspaceMetaservice.getSysWorkspaceTable(null);

        BasicBSONObject sysWsRecMatcher = new BasicBSONObject();
        if (filter.getWsName() != null) {
            sysWsRecMatcher.put(FieldName.FIELD_CLWORKSPACE_NAME, filter.getWsName());
        }
        List<Config> ret = new ArrayList<>();
        MetaCursor cursor = wsMetaTable.query(sysWsRecMatcher, null, null);
        try {
            while (cursor.hasNext()) {
                Config wsConfig = bsonConverterMgr.getMsgConverter(ScmConfigNameDefine.WORKSPACE)
                        .convertToConfig(cursor.getNext());
                ret.add(wsConfig);
            }
        }
        finally {
            cursor.close();
        }
        return ret;
    }
}
