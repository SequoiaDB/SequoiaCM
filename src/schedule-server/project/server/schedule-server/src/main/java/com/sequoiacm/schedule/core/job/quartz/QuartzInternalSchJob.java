package com.sequoiacm.schedule.core.job.quartz;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.BasicBSONObject;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.job.InternalScheduleInfo;
import com.sequoiacm.schedule.core.job.ScheduleJobInfo;
import com.sequoiacm.schedule.dao.ScheduleDao;
import com.sequoiacm.schedule.remote.ScheduleClientFactory;
import com.sequoiacm.schedule.remote.WorkerClient;

import feign.RetryableException;

public class QuartzInternalSchJob extends QuartzScheduleJob {
    private static final Logger logger = LoggerFactory.getLogger(QuartzInternalSchJob.class);

    @Override
    public void execute(ScheduleJobInfo info, JobExecutionContext context)
            throws ScheduleException, JobExecutionException {
        JobDataMap datamap = context.getJobDetail().getJobDataMap();
        InternalScheduleInfo internalSchInfo = (InternalScheduleInfo) info;
        DiscoveryClient discoverClient = (DiscoveryClient) datamap
                .get(FieldName.Schedule.FIELD_DISCOVER_CLIENT);
        ScheduleClientFactory feignClientFactory = (ScheduleClientFactory) datamap
                .get(FieldName.Schedule.FIELD_FEIGN_CLIENT_FACTORY);
        ScheduleDao scheduleDao = (ScheduleDao) datamap.get(FieldName.Schedule.FIELD_SCHEDULE_DAO);
        String workerNode = internalSchInfo.getWorkerNode();
        if (workerNode == null || workerNode.trim().isEmpty()) {
            try {
                startJobInSync(internalSchInfo, discoverClient, feignClientFactory, scheduleDao,
                        null);
            }
            catch (Exception e) {
                throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                        "failed to lunch schedule job, relunch later:" + internalSchInfo, e);
            }
            return;
        }

        if (isRunning(internalSchInfo, feignClientFactory, workerNode)) {
            logger.debug("job is health:" + internalSchInfo);
            return;
        }
        logger.warn("job is unhealth:" + internalSchInfo);
        try {
            startJobInSync(internalSchInfo, discoverClient, feignClientFactory, scheduleDao,
                    workerNode);
        }
        catch (Exception e) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "failed to lunch schedule job, relunch later:" + internalSchInfo, e);
        }
    }

    private boolean isRunning(InternalScheduleInfo internalSchInfo,
            ScheduleClientFactory feignClientFactory, String workerNode) {
        WorkerClient client = feignClientFactory.getWorkerClientByNodeUrl(workerNode);
        try {
            return client.isJobRunning(internalSchInfo.getId());
        }
        catch (RetryableException e) {
            if (e.getCause() instanceof ConnectException) {
                logger.warn("failed to connect worker, assume worker is crush:worker={}, job={}",
                        workerNode, internalSchInfo, e);
                return false;
            }
            logger.warn("failed to check job status, retry later:worker={}, job={}", workerNode,
                    internalSchInfo, e);
        }
        catch (ScheduleException e) {
            logger.warn("failed to check job status, retry later:worker={}, job={}", workerNode,
                    internalSchInfo, e);
        }

        return true;
    }

    private void startJobInSync(InternalScheduleInfo internalSchInfo,
            DiscoveryClient discoverClient, ScheduleClientFactory feignClientFactory,
            ScheduleDao scheduleDao, String exceptNode) throws Exception {
        synchronized (internalSchInfo) {
            if (internalSchInfo.isStop()) {
                return;
            }
            startJob(internalSchInfo, discoverClient, feignClientFactory, scheduleDao, exceptNode);
        }
    }

    // TODO：对于类似参数错误的调度任务需要能检测出来，不能一直跑
    private void startJob(InternalScheduleInfo internalSchInfo, DiscoveryClient discoverClient,
            ScheduleClientFactory feignClientFactory, ScheduleDao scheduleDao, String exceptNode)
            throws Exception {
        List<ServiceInstance> nodes = discoverClient
                .getInstances(internalSchInfo.getWorkerService());
        if (nodes == null || nodes.size() <= 0) {
            throw new ScheduleException(RestCommonDefine.ErrorCode.INTERNAL_ERROR,
                    "no instance for service:" + internalSchInfo.getWorkerService());
        }

        String preferRegion = internalSchInfo.getWorkerPreferRegion();
        if (preferRegion == null) {
            preferRegion = "";
        }
        String preferZone = internalSchInfo.getWorkerPreferZone();
        if (preferZone == null) {
            preferZone = "";
        }

        List<ServiceInstance> sameRegionIntances = new ArrayList<>();
        List<ServiceInstance> sameZoneIntances = new ArrayList<>();
        List<ServiceInstance> ortherIntances = new ArrayList<>();

        for (ServiceInstance node : nodes) {
            if (exceptNode != null && exceptNode.equals(node.getHost() + ":" + node.getPort())) {
                continue;
            }
            Map<String, String> meta = node.getMetadata();
            if (preferRegion.equals(meta.get("region"))) {
                if (preferZone.equals(meta.get("zone"))) {
                    sameZoneIntances.add(node);
                }
                else {
                    sameRegionIntances.add(node);
                }
            }
            else {
                ortherIntances.add(node);
            }
        }
        boolean isSuccess = false;
        if (!sameZoneIntances.isEmpty()) {
            isSuccess = startJobInSpecifyNodes(internalSchInfo, feignClientFactory, scheduleDao,
                    sameZoneIntances);
        }
        else if (!sameRegionIntances.isEmpty()) {
            isSuccess = startJobInSpecifyNodes(internalSchInfo, feignClientFactory, scheduleDao,
                    sameRegionIntances);
        }
        else {
            isSuccess = startJobInSpecifyNodes(internalSchInfo, feignClientFactory, scheduleDao,
                    ortherIntances);
        }
        if (isSuccess) {
            return;
        }
        updateWorker(scheduleDao, internalSchInfo.getId(), null, 0);
        internalSchInfo.setWorkerNode(null);
        internalSchInfo.setWorkerNodeStartTime(0);
    }

    private boolean startJobInSpecifyNodes(InternalScheduleInfo internalSchInfo,
            ScheduleClientFactory feignClientFactory, ScheduleDao scheduleDao,
            List<ServiceInstance> nodes) throws Exception {
        for (ServiceInstance node : nodes) {
            String worker = node.getHost() + ":" + node.getPort();
            long startTime = System.currentTimeMillis();
            updateWorker(scheduleDao, internalSchInfo.getId(), worker, startTime);

            try {
                WorkerClient workerClient = feignClientFactory.getWorkerClientByNodeUrl(worker);
                workerClient.startJob(internalSchInfo.getId(), internalSchInfo.getName(), startTime,
                        internalSchInfo.getJobType(), internalSchInfo.getJobData());
                logger.info("start job in remote:worker={}, job={}", worker, internalSchInfo);
            }
            catch (Exception e) {
                logger.warn("faild to start job in remote:worker={}, job={}", worker,
                        internalSchInfo, e);
                continue;
            }

            internalSchInfo.setWorkerNode(worker);
            internalSchInfo.setWorkerNodeStartTime(startTime);
            return true;
        }
        return false;
    }

    private void updateWorker(ScheduleDao dao, String schId, String worker, long startTime)
            throws Exception {
        BasicBSONObject matcher = new BasicBSONObject(FieldName.Schedule.FIELD_ID, schId);
        BasicBSONObject updator = new BasicBSONObject(FieldName.Schedule.FIELD_CONTENT + "."
                + FieldName.Schedule.FIELD_INTERNAL_WORKER_NODE, worker);
        updator.put(FieldName.Schedule.FIELD_CONTENT + "."
                + FieldName.Schedule.FIELD_INTERNAL_WORKER_START_TIME, startTime);
        updator = new BasicBSONObject("$set", updator);
        dao.update(matcher, updator);
    }
}
