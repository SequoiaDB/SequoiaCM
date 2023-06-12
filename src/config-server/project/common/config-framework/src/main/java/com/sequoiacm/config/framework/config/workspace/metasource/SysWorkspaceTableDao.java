package com.sequoiacm.config.framework.config.workspace.metasource;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.sequoiacm.config.metasource.TableDao;
import com.sequoiacm.config.metasource.exception.MetasourceException;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;

public interface SysWorkspaceTableDao extends TableDao {
    // update and return newSysWorkspaceTableDao
    public BSONObject removeDataLocation(BSONObject oldWsRecord, int siteId,
            BSONObject extraUpdator) throws MetasourceException;

    public BSONObject addDataLocation(BSONObject oldWsRecord, BSONObject location,
            BSONObject extraUpdator) throws MetasourceException;

    public BSONObject updateExternalData(BSONObject matcher, BSONObject externalData,
            BSONObject extraUpdator) throws MetasourceException;

    BSONObject updateDataLocation(BSONObject matcher, BasicBSONList locations,
            BSONObject extraUpdator) throws ScmConfigException;

    BSONObject updateDescription(BSONObject matcher, String newDesc, BSONObject extraUpdator)
            throws ScmConfigException;

    BSONObject updateSiteCacheStrategy(BSONObject matcher, String newSiteCacheStrategy,
            BSONObject extraUpdator) throws ScmConfigException;

    BSONObject updatePreferred(BSONObject matcher, String newPreferred, BSONObject extraUpdator)
            throws ScmConfigException;

    BSONObject updateDirectory(BSONObject matcher, Boolean isEnableDirectory,
            BSONObject extraUpdator) throws ScmConfigException;

    BSONObject updateMetaDomain(BSONObject matcher, String newDomain, BSONObject extraUpdator)
            throws ScmConfigException;

    BSONObject addExtraMetaCs(BSONObject matcher, String newCs, BSONObject extraUpdator)
            throws ScmConfigException;

    BSONObject updateByNewAttribute(BSONObject matcher, BSONObject newInfo, BSONObject extraUpdator)
            throws ScmConfigException;
}
