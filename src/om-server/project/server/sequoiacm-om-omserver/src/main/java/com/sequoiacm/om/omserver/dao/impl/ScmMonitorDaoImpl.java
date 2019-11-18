package com.sequoiacm.om.omserver.dao.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sequoiacm.client.common.TrafficType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmStatisticsFileDelta;
import com.sequoiacm.client.core.ScmStatisticsTraffic;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmMonitorDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmFileDeltaStatistics;
import com.sequoiacm.om.omserver.module.OmFileTrafficStatistics;
import com.sequoiacm.om.omserver.module.OmServiceInstanceInfo;
import com.sequoiacm.om.omserver.module.OmStatisticsInfo;
import com.sequoiacm.om.omserver.session.ScmOmSessionImpl;

public class ScmMonitorDaoImpl implements ScmMonitorDao {
    private ScmSession connection;

    public ScmMonitorDaoImpl(ScmOmSessionImpl session) {
        this.connection = session.getConnection();
    }

    @Override
    public List<String> getSiteList() throws ScmInternalException {
        try {
            return ScmSystem.ServiceCenter.getSiteList(connection);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get site list, " + e.getMessage(), e);
        }
    }

    @Override
    public String getRootSiteServiceName() throws ScmInternalException, ScmOmServerException {
        try {
            List<ScmServiceInstance> contentservers = ScmSystem.ServiceCenter
                    .getContentServerInstanceList(connection);
            for (ScmServiceInstance contentserver : contentservers) {
                if (contentserver.isRootSite()) {
                    return contentserver.getServiceName();
                }
            }
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get root site, " + e.getMessage(), e);
        }
        throw new ScmOmServerException(ScmOmServerError.SYSTEM_ERROR, "root site not exist");
    }

    @Override
    public OmFileTrafficStatistics getFileTraffic(String workspaceName)
            throws ScmInternalException {
        OmFileTrafficStatistics trafficsInfo = new OmFileTrafficStatistics();
        List<OmStatisticsInfo> uploadTrafficList = new ArrayList<>();
        List<OmStatisticsInfo> downloadTrafficList = new ArrayList<>();
        ScmCursor<ScmStatisticsTraffic> trafficCursor = null;
        try {
            trafficCursor = ScmSystem.Statistics.listTraffic(connection, ScmQueryBuilder
                    .start(ScmAttributeName.Traffic.WORKSPACE_NAME).is(workspaceName).get());
            while (trafficCursor.hasNext()
                    && (uploadTrafficList.size() < 30 || downloadTrafficList.size() < 30)) {
                ScmStatisticsTraffic traffic = trafficCursor.getNext();
                OmStatisticsInfo statisticsInfo = new OmStatisticsInfo(traffic.getTraffic(),
                        traffic.getRecordTime());
                if (traffic.getType() == TrafficType.FILE_DOWNLOAD
                        && downloadTrafficList.size() < 30) {
                    downloadTrafficList.add(statisticsInfo);
                }
                else if (traffic.getType() == TrafficType.FILE_UPLOAD
                        && uploadTrafficList.size() < 30) {
                    uploadTrafficList.add(statisticsInfo);
                }
            }
            trafficsInfo.setDownloadTraffics(downloadTrafficList);
            trafficsInfo.setUploadTraffics(uploadTrafficList);
            return trafficsInfo;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get file traffic, " + e.getMessage(), e);
        }
        finally {
            if (trafficCursor != null) {
                trafficCursor.close();
            }
        }
    }

    @Override
    public OmFileDeltaStatistics getFileDelta(String workspaceName) throws ScmInternalException {
        OmFileDeltaStatistics deltaInfo = new OmFileDeltaStatistics();
        List<OmStatisticsInfo> sizeDeltaList = new ArrayList<>();
        List<OmStatisticsInfo> countDeltaList = new ArrayList<>();
        ScmCursor<ScmStatisticsFileDelta> deltaCursor = null;
        try {
            deltaCursor = ScmSystem.Statistics.listFileDelta(connection, ScmQueryBuilder
                    .start(ScmAttributeName.FileDelta.WORKSPACE_NAME).is(workspaceName).get());
            while (deltaCursor.hasNext()) {
                if (sizeDeltaList.size() > 30) {
                    break;
                }
                ScmStatisticsFileDelta fileDelta = deltaCursor.getNext();
                OmStatisticsInfo countDeltaInfo = new OmStatisticsInfo(fileDelta.getCountDelta(),
                        fileDelta.getRecordTime());
                OmStatisticsInfo szieDeltaInfo = new OmStatisticsInfo(fileDelta.getSizeDelta(),
                        fileDelta.getRecordTime());
                sizeDeltaList.add(szieDeltaInfo);
                countDeltaList.add(countDeltaInfo);
            }
            deltaInfo.setCountDelta(countDeltaList);
            deltaInfo.setSizeDelta(sizeDeltaList);
            return deltaInfo;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get file delta, " + e.getMessage(), e);
        }
        finally {
            if (deltaCursor != null) {
                deltaCursor.close();
            }
        }
    }

    @Override
    public List<OmServiceInstanceInfo> getServiceInstance(String serviceName)
            throws ScmInternalException {
        List<OmServiceInstanceInfo> instances = new ArrayList<>();
        List<ScmServiceInstance> driverInstances;
        try {
            driverInstances = ScmSystem.ServiceCenter.getServiceInstanceList(connection,
                    serviceName);
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get service list, " + e.getMessage(), e);
        }

        for (ScmServiceInstance driverInstance : driverInstances) {
            instances.add(transformToOmServiceInstance(driverInstance));
        }
        return instances;
    }

    private OmServiceInstanceInfo transformToOmServiceInstance(ScmServiceInstance driverInstance) {
        OmServiceInstanceInfo instance = new OmServiceInstanceInfo();
        instance.setHost(driverInstance.getIp());
        instance.setPort(driverInstance.getPort());
        instance.setRegion(driverInstance.getRegion());
        instance.setServiceName(driverInstance.getServiceName());
        instance.setZone(driverInstance.getZone());
        instance.setContentServer(driverInstance.isContentServer());
        instance.setRootSite(driverInstance.isRootSite());
        return instance;
    }

    @Override
    public List<OmServiceInstanceInfo> getContentServerInstance() throws ScmInternalException {
        List<OmServiceInstanceInfo> serviceInstance = getServiceInstance(null);
        Iterator<OmServiceInstanceInfo> it = serviceInstance.iterator();
        while (it.hasNext()) {
            OmServiceInstanceInfo instance = it.next();
            if (!instance.isContentServer()) {
                it.remove();
            }
        }
        return serviceInstance;
    }

}
