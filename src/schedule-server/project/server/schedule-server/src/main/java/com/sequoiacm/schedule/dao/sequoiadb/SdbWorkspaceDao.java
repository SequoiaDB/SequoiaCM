package com.sequoiacm.schedule.dao.sequoiadb;

import org.bson.BSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.schedule.dao.WorkspaceDao;
import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;

@Repository("WorkspaceDao")
public class SdbWorkspaceDao implements WorkspaceDao {
    private SdbDataSourceWrapper datasource;
    private String csName = "SCMSYSTEM";
    private String clName = "WORKSPACE";

    @Autowired
    public SdbWorkspaceDao(SdbDataSourceWrapper datasource) {
        this.datasource = datasource;
    }

    @Override
    public ScmBSONObjectCursor query(BSONObject matcher) throws Exception {
        return SdbDaoCommon.query(datasource, csName, clName, matcher);
    }
}