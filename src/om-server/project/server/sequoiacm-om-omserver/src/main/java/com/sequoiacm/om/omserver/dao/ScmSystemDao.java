package com.sequoiacm.om.omserver.dao;

import java.util.Map;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmSystemDao {

    void setGlobalConfig(ScmOmSession session, String configName, String configValue)
            throws ScmInternalException;

    Map<String, String> getGlobalConfig(ScmOmSession session, String configName)
            throws ScmInternalException;
}