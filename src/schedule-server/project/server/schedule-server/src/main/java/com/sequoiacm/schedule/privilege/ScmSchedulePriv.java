package com.sequoiacm.schedule.privilege;

import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourceBuilder;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructure.security.privilege.impl.ScmPrivClient;

public class ScmSchedulePriv {
    private static ScmSchedulePriv instance = new ScmSchedulePriv();

    private ScmPrivClient client;

    private ScmSchedulePriv() {
    }

    public static ScmSchedulePriv getInstance() {
        return instance;
    }

    public void init(ScmPrivClient client, long hbInterval) {
        this.client = client;
        this.client.loadAuth();
        this.client.updateHeartbeatInterval(hbInterval);
    }

    public boolean hasPriority(String user, IResource resource, ScmPrivilegeDefine op) {
        return client.check(user, resource, op.getFlag());
    }

    public boolean hasPriority(String user, String resourceType, String resource,
            ScmPrivilegeDefine op) {
        IResource r = createResource(resourceType, resource);
        if (null == r) {
            return false;
        }

        return hasPriority(user, r, op);
    }

    public IResource createResource(String resourceType, String resource) {
        IResourceBuilder b = getResourceBuilder(resourceType);
        if (null != b) {
            return b.fromStringFormat(resource);
        }

        return null;
    }

    public IResourceBuilder getResourceBuilder(String resourceType) {
        return client.getResourceBuilder(resourceType);
    }

    public void grant(String token, ScmUser user, String roleName, IResource resource,
            String privilege) throws Exception {
        client.grant(token, user, roleName, resource, privilege);
    }

    public void revoke(String token, ScmUser user, String roleName, IResource resource,
            String privilege) throws Exception {
        client.revoke(token, user, roleName, resource, privilege);
    }
}
