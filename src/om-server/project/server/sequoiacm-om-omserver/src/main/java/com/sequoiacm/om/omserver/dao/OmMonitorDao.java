package com.sequoiacm.om.omserver.dao;

import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.om.omserver.common.InstanceStatus;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.monitor.*;
import com.sequoiacm.om.omserver.session.ScmOmSession;

import java.util.List;
import java.util.Map;

public interface OmMonitorDao {
    List<OmMonitorInstanceBasicInfo> getInstanceList(List<String> serviceCenterUrls)
            throws ScmOmServerException;

    OmHeapInfo getHeapInfo(String managementUrl) throws ScmOmServerException;

    OmConnectionInfo getConnectionInfo(String managementUrl) throws ScmOmServerException;

    OmThreadInfo getThreadInfo(String managementUrl) throws ScmOmServerException;

    OmProcessInfo getProcessInfo(String managementUrl) throws ScmOmServerException;

    Map<String, String> getConfigInfo(String managementUrl) throws ScmOmServerException;

    void deleteInstances(List<String> serviceCenterUrlsCache, String ipAddr, int port,
            ScmOmSession session) throws ScmOmServerException, ScmInternalException;

    List<String> getServiceCenterUrls() throws ScmInternalException;

    InstanceStatus getInstanceStatus(OmMonitorInstanceBasicInfo instance);
}
