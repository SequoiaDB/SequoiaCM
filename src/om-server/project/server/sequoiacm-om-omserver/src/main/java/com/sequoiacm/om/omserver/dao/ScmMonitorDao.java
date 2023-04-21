package com.sequoiacm.om.omserver.dao;

import java.util.List;

import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmDeltaStatistics;
import com.sequoiacm.om.omserver.module.OmFileTrafficStatistics;
import com.sequoiacm.om.omserver.module.OmServiceInstanceInfo;

public interface ScmMonitorDao {
    List<String> getSiteList() throws ScmInternalException;

    String getRootSiteServiceName() throws ScmInternalException, ScmOmServerException;

    OmFileTrafficStatistics getFileTraffic(String workspaceName, Long beginTime, Long endTime)
            throws ScmInternalException;

    OmDeltaStatistics getFileDelta(String workspaceName, Long beginTime, Long endTime)
            throws ScmInternalException;

    List<OmServiceInstanceInfo> getServiceInstance(String serviceName) throws ScmInternalException;

    List<OmServiceInstanceInfo> getContentServerInstance() throws ScmInternalException;

    OmDeltaStatistics getObjectDelta(String bucketName, Long beginTime, Long endTime) throws ScmInternalException;
}
