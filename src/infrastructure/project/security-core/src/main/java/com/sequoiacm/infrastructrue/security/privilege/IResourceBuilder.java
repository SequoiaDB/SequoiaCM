package com.sequoiacm.infrastructrue.security.privilege;

public interface IResourceBuilder {
    public IResourcePrivChecker createResourceChecker();

    public String toStringFormat(IResource resource);

    public IResource fromStringFormat(String resource);

    public String getResourceType();

    public IResource fromStringFormat(String resource, boolean isNeedFormat);
}
