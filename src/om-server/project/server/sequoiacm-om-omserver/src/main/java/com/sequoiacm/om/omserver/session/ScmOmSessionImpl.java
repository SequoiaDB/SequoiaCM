package com.sequoiacm.om.omserver.session;

import java.util.HashMap;
import java.util.Map;

import com.sequoiacm.client.core.ScmSession;


public class ScmOmSessionImpl extends ScmOmSession {
    private ScmSession connection;

    public ScmOmSessionImpl(ScmSession connection) {
        super(connection.getUser(), connection.getSessionId());
        this.connection = connection;
    }

    @Override
    public void close() {
        connection.close();
    }

    @Override
    public ScmSession getConnection() {
        return connection;
    }

    @Override
    public void resetServiceEndpoint(String serviceName) {
        connection.resetSiteName(serviceName.toLowerCase());
    }

}
