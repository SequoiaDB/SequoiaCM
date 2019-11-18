package com.sequoiacm.cloud.adminserver.dao;

import org.bson.BSONObject;

import com.sequoiacm.cloud.adminserver.metasource.MetaCursor;

public interface SiteDao {
    public MetaCursor query(BSONObject matcher) throws Exception;
}