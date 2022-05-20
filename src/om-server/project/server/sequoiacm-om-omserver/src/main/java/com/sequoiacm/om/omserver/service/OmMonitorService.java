package com.sequoiacm.om.omserver.service;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.monitor.*;
import com.sequoiacm.om.omserver.session.ScmOmSession;

import java.util.List;
import java.util.Map;

public interface OmMonitorService {

    List<OmMonitorInstanceInfo> listInstances() throws ScmOmServerException;

    Map<String, Object> getVersionInfo();

    OmMonitorInstanceInfo checkAndGetInstance(String instanceId) throws ScmOmServerException;

    OmConnectionInfo getConnectionInfo(String instanceId, ScmOmSession session)
            throws ScmOmServerException;

    OmHeapInfo getHeapInfo(String instanceId, ScmOmSession session) throws ScmOmServerException;

    OmThreadInfo getThreadInfo(String instanceId, ScmOmSession session) throws ScmOmServerException;

    OmProcessInfo getProcessInfo(String instanceId, ScmOmSession session)
            throws ScmOmServerException;

    Map<String, String> getConfigInfo(String instanceId, ScmOmSession session)
            throws ScmOmServerException;

    void deleteInstance(ScmOmSession session, String instanceId) throws ScmOmServerException, ScmInternalException;
}
