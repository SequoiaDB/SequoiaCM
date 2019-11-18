package com.sequoiacm.cloud.authentication.dao;

import java.util.List;

import com.sequoiacm.infrastructrue.security.core.ITransaction;
import com.sequoiacm.infrastructrue.security.core.ScmResource;

public interface IResourceDao {
    void insertResource(ScmResource resource);

    void deleteResource(ScmResource resource);

    void deleteResource(ScmResource resource, ITransaction t);

    List<ScmResource> listResources();

    public String generateResourceId();

    ScmResource getResource(String resourceType, String resource);

    void insertResource(ScmResource r, ITransaction t);

    ScmResource getResourceById(String resourceId);

    List<ScmResource> listResourcesByWorkspace(String workspaceName);
}
