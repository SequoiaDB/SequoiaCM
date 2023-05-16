package com.sequoiacm.om.omserver.service;

import java.util.Map;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmSystemService {

    void setGlobalConfig(ScmOmSession session, String configName, String configValue)
            throws ScmOmServerException, ScmInternalException;

    Map<String, String> getGlobalConfig(ScmOmSession session, String configName)
            throws ScmOmServerException, ScmInternalException;
}
