package com.sequoiacm.infrastructrue.security.privilege;


public interface IResourcePrivChecker {

    public void clear();

    public boolean addResourcePriv(IResource resource, int priv);

    public boolean checkResourcePriv(IResource resource, int op);

    public String getType();

    public int getResourcePriv(IResource resource);
}
