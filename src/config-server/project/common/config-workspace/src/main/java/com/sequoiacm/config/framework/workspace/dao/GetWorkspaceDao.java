package com.sequoiacm.config.framework.workspace.dao;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.config.metasource.sequoiadb.SequoiadbHelper;
import com.sequoiacm.infrastructure.config.core.common.BsonUtils;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.msg.*;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.common.FieldName;
import com.sequoiacm.config.framework.workspace.metasource.SysWorkspaceTableDao;
import com.sequoiacm.config.framework.workspace.metasource.WorkspaceMetaSerivce;
import com.sequoiacm.config.metasource.MetaCursor;
import com.sequoiacm.infrastructure.config.core.common.ScmConfigNameDefine;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
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

    public List<Version> getVersions(DefaultVersionFilter filter) throws ScmConfigException {
        BSONObject matcher = new BasicBSONObject();

        if (filter.getBussinessNames() != null) {
            List<Version> versions = new ArrayList<>();
            BSONObject inNames = new BasicBSONObject(SequoiadbHelper.DOLLAR_IN, filter.getBussinessNames());
            matcher.put(FieldName.FIELD_CLWORKSPACE_NAME, inNames);
            versions.addAll(queryVersions(matcher));
//            for (String bussinessName : filter.getBussinessNames()) {
//                matcher.put(FieldName.FIELD_CLWORKSPACE_NAME, bussinessName);
//                versions.addAll(queryVersions(matcher));
//            }
            return versions;
        }
        else {
            return queryVersions(matcher);
        }
    }

    private List<Version> queryVersions(BSONObject matcher) throws MetasourceException {
        List<Version> versions = new ArrayList<>();
        BSONObject selector = new BasicBSONObject();
        selector.put(FieldName.FIELD_CLWORKSPACE_VERSION, 1);
        selector.put(FieldName.FIELD_CLWORKSPACE_NAME, "");

        SysWorkspaceTableDao wsMetaTable = workspaceMetaservice.getSysWorkspaceTable(null);
        MetaCursor cursor = wsMetaTable.query(matcher, selector, null);
        try {
            while (cursor.hasNext()) {
                BSONObject versionObj = cursor.getNext();
                String businessName = BsonUtils.getStringChecked(versionObj,
                        FieldName.FIELD_CLWORKSPACE_NAME);
                Integer version = (Integer) BsonUtils.getObject(versionObj,
                        FieldName.FIELD_CLWORKSPACE_VERSION);
                DefaultVersion basicVersion = new DefaultVersion(ScmConfigNameDefine.WORKSPACE, businessName,
                        version);
                versions.add(basicVersion);
            }
            return versions;
        }
        finally {
            cursor.close();
        }
    }
}
