package com.sequoiacm.om.omserver.session;

import java.util.List;

import com.sequoiacm.om.omserver.exception.ScmInternalException;

public interface ScmOmSessionFactory {
    public ScmOmSession createSession(String username, String password) throws ScmInternalException;

    public ScmOmSession createSession() throws ScmInternalException;

    public void reinit(List<String> gatewayAddr, int readTimeout, String region, String zone)
            throws ScmInternalException;

    public boolean isDocked();
}
