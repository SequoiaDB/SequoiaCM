package com.sequoiacm.om.omserver.service.impl;

import java.util.List;

import com.sequoiacm.om.omserver.dao.ScmRoleDao;
import com.sequoiacm.om.omserver.factory.ScmRoleDaoFactory;
import com.sequoiacm.om.omserver.module.OmPrivilegeDetail;
import org.bson.BSONObject;
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

    @Autowired
    private ScmRoleDaoFactory scmRoleDaoFactory;

    @Override
    public OmRoleInfo getRole(ScmOmSession session, String rolename)
            throws ScmInternalException, ScmOmServerException {
        return scmRoleDaoFactory.createRoleDao(session).getRole(rolename);
    }

    @Override
    public void createRole(ScmOmSession session, String rolename, String description)
            throws ScmInternalException, ScmOmServerException {
        scmRoleDaoFactory.createRoleDao(session).createRole(rolename, description);
    }

    @Override
    public void deleteRole(ScmOmSession session, String rolename)
            throws ScmInternalException, ScmOmServerException {
        scmRoleDaoFactory.createRoleDao(session).deleteRole(rolename);
    }

    @Override
    public void grantPrivilege(ScmOmSession session, String rolename, String resourceType,
            String resource, String privilegeType)
            throws ScmInternalException, ScmOmServerException {
        String site = siteChooser.chooseFromAllSite();
        ScmRoleDao roleDao = scmRoleDaoFactory.createRoleDao(session);
        try {
            session.resetServiceEndpoint(site);
            roleDao.grantPrivilege(rolename, resourceType, resource, privilegeType);
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
        ScmRoleDao roleDao = scmRoleDaoFactory.createRoleDao(session);
        try {
            session.resetServiceEndpoint(site);
            roleDao.revokePrivilege(rolename, resourceType, resource, privilegeType);
        }
        catch (ScmInternalException e) {
            siteChooser.onException(e);
            throw e;
        }
    }

    @Override
    public long getRoleCount(ScmOmSession session, BSONObject condition)
            throws ScmInternalException {
        return scmRoleDaoFactory.createRoleDao(session).countRole(condition);
    }

    @Override
    public List<OmRoleBasicInfo> listRoles(ScmOmSession session, BSONObject condition, long skip,
            int limit) throws ScmInternalException, ScmOmServerException {
        return scmRoleDaoFactory.createRoleDao(session).listRoles(condition, skip, limit);
    }

    @Override
    public List<OmPrivilegeDetail> listPrivileges(ScmOmSession session, String roleName)
            throws ScmInternalException {
        return scmRoleDaoFactory.createRoleDao(session).listPrivileges(roleName);
    }
}
