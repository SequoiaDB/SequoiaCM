package com.sequoiacm.contentserver.privilege;

import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourcePrivChecker;

import java.util.HashMap;
import java.util.Map;

public class BucketResourceChecker implements IResourcePrivChecker {
    Map<String, BucketPrivilege> wsBucketPrivMap = new HashMap<>();

    @Override
    public void clear() {
        wsBucketPrivMap.clear();
    }

    @Override
    public boolean addResourcePriv(IResource resource, int priv) {
        BucketResource bucketResource = (BucketResource) resource;
        BucketPrivilege bucketPrivilege = wsBucketPrivMap.get(bucketResource.getWorkspace());
        if (null == bucketPrivilege) {
            bucketPrivilege = new BucketPrivilege();
            wsBucketPrivMap.put(bucketResource.getWorkspace(), bucketPrivilege);
        }

        return bucketPrivilege.addResourcePriv(bucketResource.getBucketName(), priv);
    }

    @Override
    public boolean checkResourcePriv(IResource resource, int op) {
        BucketResource bucketResource = (BucketResource) resource;

        BucketPrivilege bucketPrivilege = wsBucketPrivMap.get(bucketResource.getWorkspace());
        if (null != bucketPrivilege) {
            return bucketPrivilege.checkResourcePriv(bucketResource.getBucketName(), op);
        }

        return false;
    }

    @Override
    public String getType() {
        return BucketResource.TYPE;
    }

    @Override
    public int getResourcePriv(IResource resource) {
        BucketResource bucketResource = (BucketResource) resource;
        BucketPrivilege bucketPrivilege = wsBucketPrivMap.get(bucketResource.getWorkspace());
        if (null != bucketPrivilege) {
            return bucketPrivilege.getResourcePriv(bucketResource.getBucketName());
        }

        return 0;
    }

}
