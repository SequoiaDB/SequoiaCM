package com.sequoiacm.om.omserver.dao.impl;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmSiteDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmSiteInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmSiteDaoImpl implements ScmSiteDao {

    private static final Logger logger = LoggerFactory.getLogger(ScmSiteDaoImpl.class);
    private ScmOmSession session;

    public ScmSiteDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public List<OmSiteInfo> listSite(BSONObject filter, long skip, long limit)
            throws ScmOmServerException, ScmInternalException {
        ScmSession con = session.getConnection();
        List<OmSiteInfo> sites = new ArrayList<>();
        ScmCursor<ScmSiteInfo> cursor = null;
        try {
            cursor = ScmFactory.Site.listSite(con, filter, skip, limit);
            while (cursor.hasNext()) {
                ScmSiteInfo scmSite = cursor.getNext();
                sites.add(transformToOmSite(scmSite));
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to list site," + e.getMessage(),
                    e);
        }
        finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return sites;
    }

    @Override
    public long getSiteCount(BSONObject filter) throws  ScmInternalException {
        try {
            return ScmFactory.Site.count(session.getConnection(), filter);
        }
        catch (ScmException e) {
            logger.debug("failed to count site, filter={}", filter, e);
            throw new ScmInternalException(e.getError(),
                    "failed to get site count, " + e.getMessage(), e);
        }
    }

    @Override
    public String getSiteStrategy() throws ScmInternalException {
        ScmSession conn = session.getConnection();
        ScmType.SiteStrategyType strategy = null;
        try {
            strategy = ScmFactory.Site.getSiteStrategy(conn);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get site strategy," + e.getMessage(), e);
        }
        return strategy.getStrategy();
    }

    private OmSiteInfo transformToOmSite(ScmSiteInfo scmSite) {
        OmSiteInfo omSite = new OmSiteInfo();
        omSite.setId(scmSite.getId());
        omSite.setName(scmSite.getName());
        omSite.setRootSite(scmSite.isRootSite());
        omSite.setStageTag(scmSite.getStageTag());
        omSite.setDatasourceUrl(scmSite.getDataUrl());
        omSite.setDatasourceType(scmSite.getDataTypeStr());
        omSite.setDatasourceUser(scmSite.getDataUser());
        omSite.setDatasourcePwd(scmSite.getDataPasswd());
        omSite.setMetasourceUrl(scmSite.getMetaUrl());
        omSite.setMetasourceUser(scmSite.getMetaUser());
        omSite.setMetasourcePwd(scmSite.getMetaPasswd());
        if (scmSite.getDataConf() != null && scmSite.getDataConf().size() > 0) {
            omSite.setDatasourceConf(scmSite.getDataConf());
        }
        omSite.setDatasourceTypeEnum(scmSite.getDataType());
        return omSite;
    }

}
