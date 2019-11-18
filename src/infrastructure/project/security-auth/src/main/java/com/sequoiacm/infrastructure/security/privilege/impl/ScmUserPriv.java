package com.sequoiacm.infrastructure.security.privilege.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourceBuilder;
import com.sequoiacm.infrastructrue.security.privilege.IResourcePrivChecker;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;
import com.sequoiacm.infrastructrue.security.privilege.ScmResourceTypeDefine;

class ScmUserPriv {
    private String user;
    private Map<String, IResourcePrivChecker> resourceCheckerMap = new HashMap<>();

    public ScmUserPriv(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    public boolean check(IResource resource, int op) {
        IResourcePrivChecker checker = null;

        // allWorkspaceResource just check allWorkspaceResource checker
        if (ScmWsAllResource.TYPE.equals(resource.getType())) {
            checker = resourceCheckerMap.get(ScmWsAllResource.TYPE);
            if (null != checker) {
                return checker.checkResourcePriv(resource, op);
            }
            return false;
        }

        // workspaceResource:
        // 1. check workspaceResource checker
        // 2. check allWorkspaceResource checker
        if (ScmWorkspaceResource.RESOURCE_TYPE.equals(resource.getType())) {
            checker = resourceCheckerMap.get(ScmWorkspaceResource.RESOURCE_TYPE);
            if (null != checker) {
                if (checker.checkResourcePriv(resource, op)) {
                    return true;
                }
            }

            checker = resourceCheckerMap.get(ScmWsAllResource.TYPE);
            if (null != checker) {
                return checker.checkResourcePriv(resource, op);
            }

            return false;
        }

        // other resource (like directory resource):
        // 1. check workspaceResource checker
        // 2. check allWorkspaceResource checker
        // 3. check the resource checker

        checker = resourceCheckerMap.get(ScmWorkspaceResource.RESOURCE_TYPE);
        if (null != checker) {
            if (checker.checkResourcePriv(resource, op)) {
                return true;
            }
        }

        checker = resourceCheckerMap.get(ScmWsAllResource.TYPE);
        if (null != checker) {
            if (checker.checkResourcePriv(resource, op)) {
                return true;
            }
        }

        checker = resourceCheckerMap.get(resource.getType());
        if (null != checker) {
            return checker.checkResourcePriv(resource, op);
        }

        return false;
    }

    public boolean addResource(IResource resource, int priv, IResourceBuilder builder) {
        IResourcePrivChecker checker = resourceCheckerMap.get(resource.getType());
        if (null == checker) {
            checker = builder.createResourceChecker();
            resourceCheckerMap.put(resource.getType(), checker);

            Assert.isTrue(builder.getResourceType().equals(resource.getType()),
                    "factory's resource type must equals:factory.type=" + builder.getResourceType()
                            + ",type=" + resource.getType());
            Assert.isTrue(checker.getType().equals(builder.getResourceType()),
                    "factory's resource type must equals:factory.type=" + builder.getResourceType()
                            + ",checker.type=" + checker.getType());
        }

        return checker.addResourcePriv(resource, priv);
    }

    public void clear() {
        resourceCheckerMap.clear();
    }

    public boolean hasAllPriority(ScmWorkspaceResource wsResource) {
        IResourcePrivChecker checker = resourceCheckerMap.get(ScmWorkspaceResource.RESOURCE_TYPE);
        if (null != checker) {
            int priv = checker.getResourcePriv(wsResource);
            if ((priv & ScmPrivilegeDefine.ALL.getFlag()) == priv) {
                return true;
            }
        }

        checker = resourceCheckerMap.get(ScmResourceTypeDefine.TYPE_WS_ALL);
        if (null != checker) {
            int priv = checker.getResourcePriv(wsResource);
            if ((priv & ScmPrivilegeDefine.ALL.getFlag()) == priv) {
                return true;
            }
        }

        return false;
    }
}
