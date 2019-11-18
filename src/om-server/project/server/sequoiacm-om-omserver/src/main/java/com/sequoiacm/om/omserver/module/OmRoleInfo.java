package com.sequoiacm.om.omserver.module;

import java.util.List;

public class OmRoleInfo extends OmRoleBasicInfo {
    private List<OmResourceInfo> resources;

    public OmRoleInfo() {
    }

    public OmRoleInfo(String roleId, String roleName, String description,
            List<OmResourceInfo> resources) {
        super(roleId, roleName, description);
        this.resources = resources;

    }

    public List<OmResourceInfo> getResources() {
        return resources;
    }
}
