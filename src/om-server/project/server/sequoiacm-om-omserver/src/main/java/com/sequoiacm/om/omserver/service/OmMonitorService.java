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

    OmConnectionInfo getConnectionInfo(String instanceId) throws ScmOmServerException;

    OmHeapInfo getHeapInfo(String instanceId) throws ScmOmServerException;

    OmThreadInfo getThreadInfo(String instanceId) throws ScmOmServerException;

    OmProcessInfo getProcessInfo(String instanceId) throws ScmOmServerException;

    Map<String, String> getConfigInfo(String instanceId) throws ScmOmServerException;

    void deleteInstance(ScmOmSession session, String instanceId) throws ScmOmServerException, ScmInternalException;
}
