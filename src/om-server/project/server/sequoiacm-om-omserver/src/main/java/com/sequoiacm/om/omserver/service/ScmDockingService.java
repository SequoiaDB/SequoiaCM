package com.sequoiacm.om.omserver.service;

import java.util.List;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public interface ScmDockingService {
    public ScmOmSession dock(List<String> gatewayList, String region, String zone, String username,
            String password) throws ScmInternalException, ScmOmServerException;

    public boolean isDockedToScm();
}
