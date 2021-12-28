package com.sequoiacm.schedule.core.job.quartz;

import com.sequoiacm.infrastructure.discovery.ScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.discovery.ScmServiceInstance;
import com.sequoiacm.schedule.common.FieldName;
import com.sequoiacm.schedule.common.RestCommonDefine;
import com.sequoiacm.schedule.common.model.ScheduleException;
import com.sequoiacm.schedule.core.job.InternalScheduleInfo;
import com.sequoiacm.schedule.core.job.ScheduleJobInfo;
import com.sequoiacm.schedule.dao.ScheduleDao;
import com.sequoiacm.schedule.remote.ScheduleClientFactory;
import com.sequoiacm.schedule.remote.WorkerClient;
import feign.RetryableException;
import org.bson.BasicBSONObject;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.List;

public class QuartzInternalSchJob extends QuartzScheduleJob {
    private static final Logger logger = LoggerFactory.getLogger(QuartzInternalSchJob.class);

    @Override
    public void execute(ScheduleJobInfo info, JobExecutionContext context)
            throws ScheduleException, JobExecutionException {
        JobDataMap datamap = context.getJobDetail().getJobDataMap();
        InternalScheduleInfo internalSchInfo = (InternalScheduleInfo) info;
        ScmServiceDiscoveryClient discoverClient = (ScmServiceDiscoveryClient) datamap
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
            ScmServiceDiscoveryClient discoverClient, ScheduleClientFactory feignClientFactory,
            ScheduleDao scheduleDao, String exceptNode) throws Exception {
        synchronized (internalSchInfo) {
            if (internalSchInfo.isStop()) {
                return;
            }
            startJob(internalSchInfo, discoverClient, feignClientFactory, scheduleDao, exceptNode);
        }
    }

    // TODO：对于类似参数错误的调度任务需要能检测出来，不能一直跑
    private void startJob(InternalScheduleInfo internalSchInfo,
            ScmServiceDiscoveryClient discoverClient, ScheduleClientFactory feignClientFactory,
            ScheduleDao scheduleDao, String exceptNode) throws Exception {

        List<ScmServiceInstance> sameZoneInstances = discoverClient.getInstances(
                internalSchInfo.getPreferredRegion(), internalSchInfo.getPreferredZone(),
                internalSchInfo.getWorkerService());
        if (startJobInSpecifyNodes(internalSchInfo, feignClientFactory, scheduleDao,
                sameZoneInstances, exceptNode)) {
            return;
        }

        List<ScmServiceInstance> sameRegionInstances = discoverClient.getInstances(
                internalSchInfo.getPreferredRegion(), internalSchInfo.getWorkerService());
        sameRegionInstances.removeAll(sameZoneInstances);
        if (startJobInSpecifyNodes(internalSchInfo, feignClientFactory, scheduleDao,
                sameRegionInstances, exceptNode)) {
            return;
        }

        List<ScmServiceInstance> allInstance = discoverClient
                .getInstances(internalSchInfo.getWorkerService());
        allInstance.removeAll(sameRegionInstances);
        if (startJobInSpecifyNodes(internalSchInfo, feignClientFactory, scheduleDao, allInstance,
                exceptNode)) {
            return;
        }

        logger.warn(
                "no instance for start a job: schId={}, name={}, workerService={}, exceptWorkerInstance={}",
                internalSchInfo.getId(), internalSchInfo.getName(),
                internalSchInfo.getWorkerService(), exceptNode);
        updateWorker(scheduleDao, internalSchInfo.getId(), null, 0);
        internalSchInfo.setWorkerNode(null);
        internalSchInfo.setWorkerNodeStartTime(0);
    }

    private boolean startJobInSpecifyNodes(InternalScheduleInfo internalSchInfo,
            ScheduleClientFactory feignClientFactory, ScheduleDao scheduleDao,
            List<ScmServiceInstance> nodes, String exceptNode) throws Exception {
        for (ScmServiceInstance node : nodes) {
            String worker = node.getHost() + ":" + node.getPort();
            if (worker.equals(exceptNode)) {
                continue;
            }
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
        BasicBSONObject newValue = new BasicBSONObject(FieldName.Schedule.FIELD_CONTENT + "."
                + FieldName.Schedule.FIELD_INTERNAL_WORKER_NODE, worker);
        newValue.put(FieldName.Schedule.FIELD_CONTENT + "."
                + FieldName.Schedule.FIELD_INTERNAL_WORKER_START_TIME, startTime);
        dao.updateByScheduleId(schId, newValue);
    }
}
