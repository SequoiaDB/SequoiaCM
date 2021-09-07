package com.sequoiacm.om.omserver.service;

import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.session.ScmOmSession;

import java.util.List;
import java.util.Set;

public interface ServiceCenterService {

    public Set<String> listRegions(ScmOmSession session)
            throws ScmOmServerException, ScmInternalException;

    public Set<String> listZones(ScmOmSession session, String region)
            throws ScmOmServerException, ScmInternalException;

    public List<ScmServiceInstance> getServiceList(ScmOmSession session)
            throws ScmInternalException, ScmOmServerException;
}
