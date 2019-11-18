package com.sequoiacm.om.omserver.dao.impl;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmSiteDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmSiteInfo;
import com.sequoiacm.om.omserver.session.ScmOmSessionImpl;

public class ScmSiteDaoImpl implements ScmSiteDao {
    private ScmOmSessionImpl session;

    public ScmSiteDaoImpl(ScmOmSessionImpl session) {
        this.session = session;
    }

    @Override
    public List<OmSiteInfo> listSite() throws ScmOmServerException, ScmInternalException {
        ScmSession con = session.getConnection();
        List<OmSiteInfo> sites = new ArrayList<>();
        ScmCursor<ScmSiteInfo> cursor = null;
        try {
            cursor = ScmFactory.Site.listSite(con);
            while (cursor.hasNext()) {
                ScmSiteInfo scmSite = cursor.getNext();
                sites.add(transformToOmSite(scmSite));
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(), "failed to list site," + e.getMessage(),
                    e);
        }
        return sites;
    }

    private OmSiteInfo transformToOmSite(ScmSiteInfo scmSite) {
        OmSiteInfo omSite = new OmSiteInfo();
        omSite.setId(scmSite.getId());
        omSite.setName(scmSite.getName());
        omSite.setRootSite(scmSite.isRootSite());
        return omSite;
    }

}
