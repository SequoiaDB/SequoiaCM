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
            return checkPriv(resource, op, ScmWsAllResource.TYPE);
        }

        // workspaceResource:
        // 1. check workspaceResource checker
        // 2. check allWorkspaceResource checker
        if (ScmWorkspaceResource.RESOURCE_TYPE.equals(resource.getType())) {
            return checkPriv(resource, op, ScmWorkspaceResource.RESOURCE_TYPE,
                    ScmWsAllResource.TYPE);
        }

        // other resource (like directory resource):
        // 1. check workspaceResource checker
        // 2. check allWorkspaceResource checker
        // 3. check the resource checker
        return checkPriv(resource, op, ScmWorkspaceResource.RESOURCE_TYPE, ScmWsAllResource.TYPE,
                resource.getType());
    }

    private boolean checkPriv(IResource resource, int op, String... resourceTypesForCheck) {
        if (op == 0) {
            return true;
        }

        int noPrivBit = op;
        for (String resourceType : resourceTypesForCheck) {
            IResourcePrivChecker checker = resourceCheckerMap.get(resourceType);
            noPrivBit = checkPriv(checker, resource, noPrivBit);
            if (noPrivBit == 0) {
                return true;
            }
        }

        return false;
    }

    // 返回没有的权限，如输入 op = CREATE | DELETE：
    // 若用户没有这两个权限则返回 CREATE | DELETE
    // 若用户含有 CREATE 权限则返回 DELETE
    // 若用户含有 DELETE 权限则返回 CREATE
    // 若用户同时用拥有两个权限则返回 0
    private int checkPriv(IResourcePrivChecker checker, IResource resource, int op) {
        if (null == checker) {
            return op;
        }

        int v = checker.getResourcePriv(resource);

        // op & v = 用户在 op 上拥有的权限
        // （op & v） ^ op = 用户在 op 上没有的权限

        // 如 ： op = 1001，v = 1100
        // op(1001) & v(1100) = 1000 (用户对于 op 操作只有第一位权限)
        // 1000 ^ op(1001) = 0001 (用户对于 op 操作缺少最后一位权限)
        return op & v ^ op;
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
