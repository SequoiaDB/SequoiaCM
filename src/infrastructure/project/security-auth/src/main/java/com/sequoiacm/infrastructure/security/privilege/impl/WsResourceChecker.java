package com.sequoiacm.infrastructure.security.privilege.impl;

import java.util.HashMap;
import java.util.Map;

import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourcePrivChecker;
import com.sequoiacm.infrastructrue.security.privilege.ScmPrivilegeDefine;


public class WsResourceChecker implements IResourcePrivChecker {
    Map<String, Integer> wsPrivMap = new HashMap<>();

    WsResourceChecker() {
    }

    @Override
    public void clear() {
        wsPrivMap.clear();
    }

    @Override
    public String getType() {
        return ScmWorkspaceResource.RESOURCE_TYPE;
    }

    @Override
    public boolean addResourcePriv(IResource resource, int privilege) {
        ScmWorkspaceResource wsResource = (ScmWorkspaceResource) resource;
        String wsName = wsResource.getWorkspace();
        Integer v = wsPrivMap.get(wsName);
        if (null != v) {
            wsPrivMap.put(wsName, privilege | v);
        }
        else {
            wsPrivMap.put(wsName, privilege);
        }

        return true;
    }

    @Override
    public boolean checkResourcePriv(IResource resource, int op) {
        String wsName = resource.getWorkspace();

        Integer v = wsPrivMap.get(wsName);
        if (null != v) {
            return (op & v) == op;
        }

        return false;
    }

    public static void main(String[] args) {
        IResource a = new ScmWorkspaceResource("a");
        WsResourceChecker checker = new WsResourceChecker();
        checker.addResourcePriv(a, ScmPrivilegeDefine.READ.getFlag());
        checker.addResourcePriv(a, ScmPrivilegeDefine.CREATE.getFlag());

        System.out.println(checker.checkResourcePriv(a, ScmPrivilegeDefine.UPDATE.getFlag()));
        System.out.println(checker.checkResourcePriv(new ScmWorkspaceResource("b"),
                ScmPrivilegeDefine.UPDATE.getFlag()));
        System.out.println(checker.checkResourcePriv(a, ScmPrivilegeDefine.READ.getFlag()));
        System.out.println(checker.checkResourcePriv(a, ScmPrivilegeDefine.ALL.getFlag()));
        System.out.println(checker.checkResourcePriv(a, ScmPrivilegeDefine.READ.getFlag()
                | ScmPrivilegeDefine.CREATE.getFlag()));
        System.out.println(checker.checkResourcePriv(a, ScmPrivilegeDefine.READ.getFlag()
                | ScmPrivilegeDefine.UPDATE.getFlag()));
    }

    @Override
    public int getResourcePriv(IResource resource) {
        String wsName = resource.getWorkspace();

        Integer v = wsPrivMap.get(wsName);
        if (null != v) {
            return v;
        }

        return 0;
    }
}
