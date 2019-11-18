package com.sequoiacm.om.omserver.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequoiacm.om.omserver.core.ScmSiteChooser;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmRoleBasicInfo;
import com.sequoiacm.om.omserver.module.OmRoleInfo;
import com.sequoiacm.om.omserver.service.ScmRoleService;
import com.sequoiacm.om.omserver.session.ScmOmSession;

@Service
public class ScmRoleServiceImpl implements ScmRoleService {

    @Autowired
    private ScmSiteChooser siteChooser;

    @Override
    public OmRoleInfo getRole(ScmOmSession session, String rolename)
            throws ScmInternalException, ScmOmServerException {
        return session.getRoleDao().getRole(rolename);
    }

    @Override
    public void createRole(ScmOmSession session, String rolename, String description)
            throws ScmInternalException, ScmOmServerException {
        session.getRoleDao().createRole(rolename, description);
    }

    @Override
    public void deleteRole(ScmOmSession session, String rolename)
            throws ScmInternalException, ScmOmServerException {
        session.getRoleDao().deleteRole(rolename);
    }

    @Override
    public void grantPrivilege(ScmOmSession session, String rolename, String resourceType,
            String resource, String privilegeType)
            throws ScmInternalException, ScmOmServerException {
        String site = siteChooser.chooseFromAllSite();
        try {
            synchronized (session) {
                session.resetServiceEndpoint(site);
                session.getRoleDao().grantPrivilege(rolename, resourceType, resource,
                        privilegeType);
            }
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public void revokePrivilege(ScmOmSession session, String rolename, String resourceType,
            String resource, String privilegeType)
            throws ScmInternalException, ScmOmServerException {
        String site = siteChooser.chooseFromAllSite();
        try {
            synchronized (session) {
                session.resetServiceEndpoint(site);
                session.getRoleDao().revokePrivilege(rolename, resourceType, resource,
                        privilegeType);
            }
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public List<OmRoleBasicInfo> listRoles(ScmOmSession session, long skip, int limit)
            throws ScmInternalException, ScmOmServerException {
        return session.getRoleDao().listRoles(skip, limit);
    }
}
