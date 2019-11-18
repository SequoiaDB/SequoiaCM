package com.sequoiacm.contentserver.privilege;

import com.sequoiacm.infrastructrue.security.privilege.IResource;
import com.sequoiacm.infrastructrue.security.privilege.IResourceBuilder;
import com.sequoiacm.infrastructrue.security.privilege.IResourcePrivChecker;

public class DirResourceBuilder implements IResourceBuilder {

    @Override
    public IResourcePrivChecker createResourceChecker() {
        return new DirPrivChecker();
    }

    @Override
    public String toStringFormat(IResource resource) {
        return resource.toStringFormat();
    }

    @Override
    public IResource fromStringFormat(String resource) {
        String[] parsedDir = resource.split(":");
        if (null != parsedDir && parsedDir.length >= 2) {
            return new DirResource(parsedDir[0], parsedDir[1]);
        }

        return null;
    }

    @Override
    public String getResourceType() {
        return DirResource.RESOURCE_TYPE;
    }

}
