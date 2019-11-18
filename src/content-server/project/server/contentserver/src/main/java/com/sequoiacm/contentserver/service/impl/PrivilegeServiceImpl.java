package com.sequoiacm.contentserver.service.impl;

import org.bson.BSONObject;
import org.springframework.stereotype.Service;

import com.sequoiacm.contentserver.exception.ScmInvalidArgumentException;
import com.sequoiacm.contentserver.exception.ScmServerException;
import com.sequoiacm.contentserver.model.ScmWorkspaceInfo;
import com.sequoiacm.contentserver.privilege.DirResource;
import com.sequoiacm.contentserver.privilege.ScmFileServicePriv;
import com.sequoiacm.contentserver.service.IPrivilegeService;
import com.sequoiacm.contentserver.site.ScmContentServer;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourceBuilder;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmWsAllResource;

@Service
public class PrivilegeServiceImpl implements IPrivilegeService {

    private IResource checkResource(String resourceType, String resource)
            throws ScmServerException {
        IResourceBuilder b = ScmFileServicePriv.getInstance().getResourceBuilder(resourceType);
        if (null == b) {
            throw new ScmInvalidArgumentException(
                    "unkown resource type:resource_type=" + resourceType);
        }

        IResource r = b.fromStringFormat(resource);
        if (null == r) {
            throw new ScmInvalidArgumentException("unkown resource:resource=" + resource);
        }

        if (ScmWsAllResource.TYPE == b.getResourceType()) {
            return r;
        }

        ScmContentServer contentserver = ScmContentServer.getInstance();
        ScmWorkspaceInfo wsInfo = contentserver.getWorkspaceInfo(r.getWorkspace());
        if (null == wsInfo) {
            throw new ScmServerException(ScmError.WORKSPACE_NOT_EXIST,
                    "workspace is not exist:resource=" + resource);
        }

        if (DirResource.RESOURCE_TYPE.equals(resourceType)) {
            DirResource d = (DirResource) r;
            BSONObject destDir = contentserver.getMetaService().getDirByPath(d.getWorkspace(),
                    d.getDirectory());
            if (null == destDir) {
                throw new ScmServerException(ScmError.DIR_NOT_FOUND,
                        "directory is not exist:resource=" + resource);
            }
        }

        return r;
    }

    @Override
    public void grant(String token, ScmUser user, String roleName, String resourceType,
            String resource, String privilege) throws ScmServerException {

        IResource r = checkResource(resourceType, resource);
        try {
            ScmFileServicePriv.getInstance().grant(token, user, roleName, r, privilege);
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.PRIVILEGE_GRANT_FAILED,
                    "grant failed:user=" + user.getUsername() + ",roleName=" + roleName
                            + ",resourceType=" + resourceType + ",resource=" + resource
                            + ",privilege=" + privilege,
                    e);
        }
    }

    @Override
    public void revoke(String token, ScmUser user, String roleName, String resourceType,
            String resource, String privilege) throws ScmServerException {
        IResource r = checkResource(resourceType, resource);
        try {
            ScmFileServicePriv.getInstance().revoke(token, user, roleName, r, privilege);
        }
        catch (Exception e) {
            throw new ScmServerException(ScmError.PRIVILEGE_REVOKE_FAILED,
                    "revoke failed:user=" + user.getUsername() + ",roleName=" + roleName
                            + ",resourceType=" + resourceType + ",resource=" + resource
                            + ",privilege=" + privilege,
                    e);
        }
    }
}