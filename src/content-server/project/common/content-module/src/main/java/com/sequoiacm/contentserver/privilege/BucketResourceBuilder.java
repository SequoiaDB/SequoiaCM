package com.sequoiacm.contentserver.privilege;

import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourceBuilder;
import com.sequoiacm.infrastructrue.security.privilege.IResourcePrivChecker;

public class BucketResourceBuilder implements IResourceBuilder {
    @Override
    public IResourcePrivChecker createResourceChecker() {
        return new BucketResourceChecker();
    }

    @Override
    public String toStringFormat(IResource resource) {
        return resource.toStringFormat();
    }

    @Override
    public IResource fromStringFormat(String resource) {
        return fromStringFormat(resource, false);
    }

    @Override
    public String getResourceType() {
        return BucketResource.TYPE;
    }

    @Override
    public IResource fromStringFormat(String resource, boolean isNeedFormat) {
        String[] bucketArr = resource.split(":");
        if (bucketArr.length >= 2) {
            return new BucketResource(bucketArr[0], bucketArr[1]);
        }
        return null;
    }
}
