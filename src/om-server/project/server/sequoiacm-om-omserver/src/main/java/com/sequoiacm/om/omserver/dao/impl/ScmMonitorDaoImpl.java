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
import com.sequoiacm.client.core.ScmStatisticsObjectDelta;
import com.sequoiacm.client.core.ScmStatisticsTraffic;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.om.omserver.dao.ScmMonitorDao;
import com.sequoiacm.om.omserver.exception.ScmInternalException;
import com.sequoiacm.om.omserver.exception.ScmOmServerError;
import com.sequoiacm.om.omserver.exception.ScmOmServerException;
import com.sequoiacm.om.omserver.module.OmDeltaStatistics;
import com.sequoiacm.om.omserver.module.OmFileTrafficStatistics;
import com.sequoiacm.om.omserver.module.OmServiceInstanceInfo;
import com.sequoiacm.om.omserver.module.OmStatisticsInfo;
import com.sequoiacm.om.omserver.session.ScmOmSession;

public class ScmMonitorDaoImpl implements ScmMonitorDao {
    private ScmSession connection;

    public ScmMonitorDaoImpl(ScmOmSession session) {
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
    public OmFileTrafficStatistics getFileTraffic(String workspaceName, Long beginTime,
            Long endTime)
            throws ScmInternalException {
        OmFileTrafficStatistics trafficsInfo = new OmFileTrafficStatistics();
        List<OmStatisticsInfo> uploadTrafficList = new ArrayList<>();
        List<OmStatisticsInfo> downloadTrafficList = new ArrayList<>();
        int maxRecordCount = 30;
        if (beginTime != null || endTime != null) {
            maxRecordCount = Integer.MAX_VALUE;
        }
        ScmCursor<ScmStatisticsTraffic> trafficCursor = null;
        try {
            ScmQueryBuilder queryBuilder = ScmQueryBuilder
                    .start(ScmAttributeName.Traffic.WORKSPACE_NAME).is(workspaceName);
            if (beginTime != null) {
                queryBuilder.and(ScmAttributeName.Traffic.RECORD_TIME).greaterThanEquals(beginTime);
            }
            if (endTime != null) {
                queryBuilder.and(ScmAttributeName.Traffic.RECORD_TIME).lessThanEquals(endTime);
            }
            trafficCursor = ScmSystem.Statistics.listTraffic(connection, queryBuilder.get());
            while (trafficCursor.hasNext()
                    && (uploadTrafficList.size() <= maxRecordCount
                            || downloadTrafficList.size() <= maxRecordCount)) {
                ScmStatisticsTraffic traffic = trafficCursor.getNext();
                OmStatisticsInfo statisticsInfo = new OmStatisticsInfo(traffic.getTraffic(),
                        traffic.getRecordTime());
                if (traffic.getType() == TrafficType.FILE_DOWNLOAD
                        && downloadTrafficList.size() <= maxRecordCount) {
                    downloadTrafficList.add(statisticsInfo);
                }
                else if (traffic.getType() == TrafficType.FILE_UPLOAD
                        && uploadTrafficList.size() <= maxRecordCount) {
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
    public OmDeltaStatistics getFileDelta(String workspaceName, Long beginTime, Long endTime)
            throws ScmInternalException {
        OmDeltaStatistics deltaInfo = new OmDeltaStatistics();
        List<OmStatisticsInfo> sizeDeltaList = new ArrayList<>();
        List<OmStatisticsInfo> countDeltaList = new ArrayList<>();
        ScmCursor<ScmStatisticsFileDelta> deltaCursor = null;
        int maxRecordCount = 30;
        if (beginTime != null || endTime != null) {
            maxRecordCount = Integer.MAX_VALUE;
        }
        try {
            ScmQueryBuilder queryBuilder = ScmQueryBuilder
                    .start(ScmAttributeName.Traffic.WORKSPACE_NAME).is(workspaceName);
            if (beginTime != null) {
                queryBuilder.and(ScmAttributeName.Traffic.RECORD_TIME).greaterThanEquals(beginTime);
            }
            if (endTime != null) {
                queryBuilder.and(ScmAttributeName.Traffic.RECORD_TIME).lessThanEquals(endTime);
            }
            deltaCursor = ScmSystem.Statistics.listFileDelta(connection, queryBuilder.get());
            while (deltaCursor.hasNext()) {
                if (sizeDeltaList.size() > maxRecordCount) {
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
    public OmDeltaStatistics getObjectDelta(String bucketName, Long beginTime, Long endTime)
            throws ScmInternalException {
        OmDeltaStatistics deltaInfo = new OmDeltaStatistics();
        List<OmStatisticsInfo> sizeDeltaList = new ArrayList<>();
        List<OmStatisticsInfo> countDeltaList = new ArrayList<>();
        ScmCursor<ScmStatisticsObjectDelta> deltaCursor = null;
        int maxRecordCount = 30;
        if (beginTime != null || endTime != null) {
            maxRecordCount = Integer.MAX_VALUE;
        }
        try {
            ScmQueryBuilder queryBuilder = ScmQueryBuilder
                    .start(ScmAttributeName.ObjectDelta.BUCKET_NAME).is(bucketName);
            if (beginTime != null) {
                queryBuilder.and(ScmAttributeName.Traffic.RECORD_TIME).greaterThanEquals(beginTime);
            }
            if (endTime != null) {
                queryBuilder.and(ScmAttributeName.Traffic.RECORD_TIME).lessThanEquals(endTime);
            }
            deltaCursor = ScmSystem.Statistics.listObjectDelta(connection, queryBuilder.get());
            while (deltaCursor.hasNext()) {
                if (sizeDeltaList.size() > maxRecordCount) {
                    break;
                }
                ScmStatisticsObjectDelta fileDelta = deltaCursor.getNext();
                OmStatisticsInfo countDeltaInfo = new OmStatisticsInfo(fileDelta.getCountDelta(),
                        fileDelta.getRecordTime());
                OmStatisticsInfo sizeDeltaInfo = new OmStatisticsInfo(fileDelta.getSizeDelta(),
                        fileDelta.getRecordTime());
                sizeDeltaList.add(sizeDeltaInfo);
                countDeltaList.add(countDeltaInfo);
            }
            deltaInfo.setCountDelta(countDeltaList);
            deltaInfo.setSizeDelta(sizeDeltaList);
            return deltaInfo;
        }
        catch (ScmException e) {
            throw new ScmInternalException(e.getError(),
                    "failed to get object delta, " + e.getMessage(), e);
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
