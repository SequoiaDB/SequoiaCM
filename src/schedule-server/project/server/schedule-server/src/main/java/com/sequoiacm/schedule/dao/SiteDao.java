package com.sequoiacm.schedule.dao;

import org.bson.BSONObject;

import com.sequoiacm.schedule.entity.ScmBSONObjectCursor;
import com.sequoiacm.schedule.entity.SiteEntity;

public interface SiteDao {
    public ScmBSONObjectCursor query(BSONObject matcher) throws Exception;

    public SiteEntity queryOne(String siteName) throws Exception;
}