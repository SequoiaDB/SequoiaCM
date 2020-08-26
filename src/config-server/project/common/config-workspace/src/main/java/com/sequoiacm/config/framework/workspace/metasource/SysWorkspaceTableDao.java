package com.sequoiacm.config.framework.workspace.metasource;

import org.bson.BSONObject;

import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.exception.MetasourceException;

public interface SysWorkspaceTableDao extends TableDao {
    // update and return new
    public BSONObject removeDataLocation(BSONObject oldWsRecord, int siteId)
            throws MetasourceException;

    public BSONObject addDataLocation(BSONObject oldWsRecord, BSONObject location)
            throws MetasourceException;

    public BSONObject updateExternalData(BSONObject matcher, BSONObject externalData)
            throws MetasourceException;
}
