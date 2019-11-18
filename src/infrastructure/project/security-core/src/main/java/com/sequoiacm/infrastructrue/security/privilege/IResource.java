package com.sequoiacm.infrastructrue.security.privilege;

public interface IResource {
    String getType();

    String getWorkspace();

    String toStringFormat();
}
