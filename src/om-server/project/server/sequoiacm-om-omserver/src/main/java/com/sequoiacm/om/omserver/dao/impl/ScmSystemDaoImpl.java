package com.sequoiacm.om.omserver.dao.impl;

import java.util.HashMap;
import java.util.Map;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmSystemDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public class ScmSystemDaoImpl implements ScmSystemDao {

    private ScmOmSession session;

    public ScmSystemDaoImpl(ScmOmSession session) {
        this.session = session;
    }

    @Override
    public void setGlobalConfig(ScmOmSession session, String configName, String configValue)
            throws ScmInternalException {
        ScmSession conn = session.getConnection();
        try {
            ScmSystem.Configuration.setGlobalConfig(conn, configName, configValue);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "Failed to set global configuration, " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getGlobalConfig(ScmOmSession session, String configName)
            throws ScmInternalException {
        ScmSession conn = session.getConnection();
        try {
            if (configName == null) {
                return ScmSystem.Configuration.getGlobalConfig(conn);
            }
            Map<String, String> res = new HashMap<>();
            String configValue = ScmSystem.Configuration.getGlobalConfig(conn, configName);
            res.put(configName, configValue);
            return res;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "Failed to get global configuration, " + e.getMessage(), e);
        }
    }
}
