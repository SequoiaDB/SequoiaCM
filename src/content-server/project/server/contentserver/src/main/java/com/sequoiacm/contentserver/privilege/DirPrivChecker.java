package com.sequoiacm.contentserver.privilege;

import java.util.HashMap;
import java.util.Map;

import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourcePrivChecker;

class DirPrivChecker implements IResourcePrivChecker {
    Map<String, DirPriv> wsDirPrivMap = new HashMap<>();

    @Override
    public void clear() {
        wsDirPrivMap.clear();
    }

    @Override
    public boolean addResourcePriv(IResource resource, int priv) {
        DirResource dirResource = (DirResource) resource;
        DirPriv dirPriv = wsDirPrivMap.get(dirResource.getWorkspace());
        if (null == dirPriv) {
            dirPriv = new DirPriv();
            wsDirPrivMap.put(dirResource.getWorkspace(), dirPriv);
        }

        return dirPriv.addResourcePriv(dirResource.getDirectory(), priv);
    }

    @Override
    public boolean checkResourcePriv(IResource resource, int op) {
        DirResource dirResource = (DirResource) resource;

        DirPriv dirPriv = wsDirPrivMap.get(dirResource.getWorkspace());
        if (null != dirPriv) {
            return dirPriv.checkResourcePriv(dirResource.getDirectory(), op);
        }

        return false;
    }

    @Override
    public String getType() {
        return DirResource.RESOURCE_TYPE;
    }

    @Override
    public int getResourcePriv(IResource resource) {
        DirResource dirResource = (DirResource) resource;

        DirPriv dirPriv = wsDirPrivMap.get(dirResource.getWorkspace());
        if (null != dirPriv) {
            return dirPriv.getResourcePriv(dirResource.getDirectory());
        }

        return 0;
    }

}
