package com.sequoiacm.config.framework.workspace.metasource;

import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import org.bson.BSONCallback;
import org.bson.BSONObject;

import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import org.bson.types.BasicBSONList;

import java.util.List;

public interface SysWorkspaceTableDao extends TableDao {
    // update and return newSysWorkspaceTableDao
    public BSONObject removeDataLocation(BSONObject oldWsRecord, int siteId, BSONObject versionSet)
            throws MetasourceException;

    public BSONObject addDataLocation(BSONObject oldWsRecord, BSONObject location,
            BSONObject versionSet) throws MetasourceException;

    public BSONObject updateExternalData(BSONObject matcher, BSONObject externalData,
            BSONObject versionSet) throws MetasourceException;

    BSONObject updateDataLocation(BSONObject matcher, BasicBSONList locations,
            BSONObject versionSet) throws ScmConfigException;

    BSONObject updateDescription(BSONObject matcher, String newDesc, BSONObject versionSet)
            throws ScmConfigException;

    BSONObject updateSiteCacheStrategy(BSONObject matcher, String newSiteCacheStrategy,
            BSONObject versionSet) throws ScmConfigException;

    BSONObject updatePreferred(BSONObject matcher, String newPreferred, BSONObject versionSet)
            throws ScmConfigException;
}
