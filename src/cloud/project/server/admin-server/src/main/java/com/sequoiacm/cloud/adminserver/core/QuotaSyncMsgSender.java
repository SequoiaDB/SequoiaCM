package com.sequoiacm.cloud.adminserver.core;

import com.sequoiacm.cloud.adminserver.exception.StatisticsError;
import com.sequoiacm.cloud.adminserver.exception.StatisticsException;
import com.sequoiacm.cloud.adminserver.remote.QuotaSyncNotifyServerClientFactory;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.infrastructure.discovery.EnableScmServiceDiscoveryClient;
import com.sequoiacm.infrastructure.discovery.ScmServiceInstance;
import com.sequoiacm.infrastructure.feign.ScmFeignException;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@EnableScmServiceDiscoveryClient
public class QuotaSyncMsgSender {
    private static final Logger logger = LoggerFactory.getLogger(QuotaSyncMsgSender.class);

    private ThreadPoolExecutor msgSenderThreadPool;

    private QuotaSyncNotifyServerClientFactory clientFactory;

    @Autowired
    public QuotaSyncMsgSender(QuotaSyncNotifyServerClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        msgSenderThreadPool = new ThreadPoolExecutor(1, 20, 60L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());

    }

    @PreDestroy
    public void destroy() {
        if (msgSenderThreadPool != null) {
            msgSenderThreadPool.shutdown();
        }
    }

    public StartSyncMsgResponse sendStartSyncMsg(final String type, final String name,
            final int syncRoundNumber, int quotaRoundNumber, long expireTime,
            List<ScmServiceInstance> notifyInstances) throws StatisticsException {

        final StartSyncMsgResponse response = new StartSyncMsgResponse();
        List<Future<Result>> futures = new ArrayList<>();
        for (ScmServiceInstance notifyInstance : notifyInstances) {
            final String nodeUrl = notifyInstance.getHost() + ":" + notifyInstance.getPort();
            Future<Result> future = msgSenderThreadPool.submit(new StartSyncTask(nodeUrl, type,
                    name, syncRoundNumber, quotaRoundNumber, expireTime));
            futures.add(future);
        }
        StatisticsException lastException = null;
        for (Future<Result> future : futures) {
            try {
                Result result = future.get();
                response.addResult(result);
            }
            catch (ExecutionException e) {
                if (e.getCause() instanceof StatisticsException) {
                    lastException = (StatisticsException) e.getCause();
                }
                else {
                    lastException = new StatisticsException(StatisticsError.INTERNAL_ERROR,
                            "failed to send startSync msg", e);
                }
            }
            catch (Exception e) {
                lastException = new StatisticsException(StatisticsError.INTERNAL_ERROR,
                        "failed to send startSync msg", e);
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        return response;
    }

    public void sendSetAgreementTimeMsg(String type, String name, int syncRoundNumber,
            int quotaRoundNumber, long agreementTime, List<Result> results)
            throws StatisticsException {
        try {
            List<Future<?>> futures = new ArrayList<>(results.size());
            for (Result result : results) {
                Future<?> future = msgSenderThreadPool
                        .submit(new SetAgreementTimeTask(result.getNodeUrl(), type, name,
                                syncRoundNumber, quotaRoundNumber, agreementTime));
                futures.add(future);
            }
            StatisticsException lastException = null;
            for (Future<?> future : futures) {
                try {
                    future.get();
                }
                catch (ExecutionException e) {
                    if (e.getCause() instanceof StatisticsException) {
                        lastException = (StatisticsException) e.getCause();
                    }
                    else {
                        lastException = new StatisticsException(StatisticsError.INTERNAL_ERROR,
                                "failed to send setAgreementTime msg", e);
                    }
                }
                catch (Exception e) {
                    lastException = new StatisticsException(StatisticsError.INTERNAL_ERROR,
                            "failed to send setAgreementTime msg", e);
                }
            }
            if (lastException != null) {
                throw lastException;
            }
        }
        catch (StatisticsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                    "failed to send setAgreementTime msg", e);
        }

    }

    public void sendCancelSyncMsgSilence(String type, String name, int syncRoundNumber,
            int quotaRoundNumber, List<ScmServiceInstance> notifyInstances) {
        try {
            List<Future<?>> futures = new ArrayList<>(notifyInstances.size());
            for (ScmServiceInstance notifyInstance : notifyInstances) {
                String nodeUrl = notifyInstance.getHost() + ":" + notifyInstance.getPort();
                Future<?> future = msgSenderThreadPool.submit(new CancelSyncTaskIgnoreError(nodeUrl,
                        type, name, syncRoundNumber, quotaRoundNumber));
                futures.add(future);
            }
            for (Future<?> future : futures) {
                future.get();

            }
        }
        catch (Exception e) {
            logger.warn("failed to send cancel sync msg", e);
        }
    }

    public void sendFinishSyncMsgSilence(String type, String name, int syncRoundNumber,
            int quotaRoundNumber, List<ScmServiceInstance> notifyInstances) {
        try {
            List<Future<?>> futures = new ArrayList<>(notifyInstances.size());
            for (ScmServiceInstance notifyInstance : notifyInstances) {
                String nodeUrl = notifyInstance.getHost() + ":" + notifyInstance.getPort();
                Future<?> future = msgSenderThreadPool.submit(new FinishSyncTaskIgnoreError(nodeUrl,
                        type, name, syncRoundNumber, quotaRoundNumber));
                futures.add(future);
            }
            for (Future<?> future : futures) {
                future.get();
            }
        }
        catch (Exception e) {
            logger.warn("failed to send finish sync msg", e);
        }
    }

    private class StartSyncTask implements Callable<Result> {

        private String nodeUrl;
        private String type;
        private String name;
        private int syncRoundNumber;
        private int quotaRoundNumber;
        private long expireTime;

        public StartSyncTask(String nodeUrl, String type, String name, int syncRoundNumber,
                int quotaRoundNumber, long expireTime) {
            this.nodeUrl = nodeUrl;
            this.type = type;
            this.name = name;
            this.syncRoundNumber = syncRoundNumber;
            this.quotaRoundNumber = quotaRoundNumber;
            this.expireTime = expireTime;
        }

        @Override
        public Result call() throws Exception {
            try {
                BSONObject bsonObject = clientFactory.getClient(nodeUrl).beginSync(type, name,
                        syncRoundNumber, quotaRoundNumber, expireTime);
                long nodeTime = BsonUtils
                        .getNumberChecked(bsonObject, CommonDefine.RestArg.QUOTA_SYNC_CURRENT_TIME)
                        .longValue();
                return new Result(nodeUrl, nodeTime);
            }
            catch (ScmFeignException e) {
                String errorMsg = "failed to send sync msg to node " + nodeUrl + ",";
                if (e.getStatus() == HttpStatus.NOT_FOUND.value()) {
                    errorMsg += "node version may be too old";
                }
                else {
                    errorMsg += e.getMessage();
                }
                throw new StatisticsException(StatisticsError.INTERNAL_ERROR, errorMsg, e);
            }
            catch (Exception e) {
                String errorMsg = "failed to send sync msg to node " + nodeUrl + ", "
                        + e.getMessage();
                throw new StatisticsException(StatisticsError.INTERNAL_ERROR, errorMsg, e);
            }
        }
    }

    private class CancelSyncTaskIgnoreError implements Runnable {
        private String nodeUrl;
        private String type;
        private String name;
        private int syncRoundNumber;
        private int quotaRoundNumber;

        public CancelSyncTaskIgnoreError(String nodeUrl, String type, String name,
                int syncRoundNumber, int quotaRoundNumber) {
            this.nodeUrl = nodeUrl;
            this.type = type;
            this.name = name;
            this.syncRoundNumber = syncRoundNumber;
            this.quotaRoundNumber = quotaRoundNumber;
        }

        @Override
        public void run() {
            try {
                clientFactory.getClient(nodeUrl).cancelSync(type, name, syncRoundNumber,
                        quotaRoundNumber);
            }
            catch (Exception e) {
                logger.warn(
                        "failed to send cancelSync msg to node:{},type={},name={},syncRoundNumber={},quotaRoundNumber={}",
                        nodeUrl, type, name, syncRoundNumber, quotaRoundNumber, e);
            }
        }
    }

    private class SetAgreementTimeTask implements Callable<Void> {

        private String nodeUrl;
        private String type;
        private String name;
        private int syncRoundNumber;
        private int quotaRoundNumber;
        private long agreementTime;

        public SetAgreementTimeTask(String nodeUrl, String type, String name, int syncRoundNumber,
                int quotaRoundNumber, long agreementTime) {
            this.nodeUrl = nodeUrl;
            this.type = type;
            this.name = name;
            this.syncRoundNumber = syncRoundNumber;
            this.quotaRoundNumber = quotaRoundNumber;
            this.agreementTime = agreementTime;
        }

        @Override
        public Void call() throws StatisticsException {
            try {
                clientFactory.getClient(nodeUrl).setAgreementTime(type, name, agreementTime,
                        syncRoundNumber, quotaRoundNumber);
            }
            catch (Exception e) {
                throw new StatisticsException(StatisticsError.INTERNAL_ERROR,
                        "failed to send setAgreementTime msg to node " + nodeUrl + ":type=" + type
                                + ",name=" + name + ",syncRoundNumber=" + syncRoundNumber
                                + ",quotaRoundNumber=" + quotaRoundNumber + ",agreementTime="
                                + agreementTime,
                        e);
            }
            return null;
        }
    }

    private class FinishSyncTaskIgnoreError implements Runnable {
        private String nodeUrl;
        private String type;
        private String name;
        private int syncRoundNumber;
        private int quotaRoundNumber;

        public FinishSyncTaskIgnoreError(String nodeUrl, String type, String name,
                int syncRoundNumber, int quotaRoundNumber) {
            this.nodeUrl = nodeUrl;
            this.type = type;
            this.name = name;
            this.syncRoundNumber = syncRoundNumber;
            this.quotaRoundNumber = quotaRoundNumber;
        }

        @Override
        public void run() {
            try {
                clientFactory.getClient(nodeUrl).finishSync(type, name, syncRoundNumber,
                        quotaRoundNumber);
            }
            catch (Exception e) {
                logger.warn(
                        "failed to send finishSyncMsg to node:{},type={},name={},syncRoundNumber={},quotaRoundNumber={}",
                        nodeUrl, type, name, syncRoundNumber, quotaRoundNumber, e);
            }
        }
    }

    public static class StartSyncMsgResponse {

        private List<Result> results = new ArrayList<>();

        private boolean sorted;

        public List<Result> getResults() {
            return results;
        }

        public void addResult(Result result) {
            this.results.add(result);
        }
    }

    public static class Result {
        private String nodeUrl;
        private long nodeTime;

        public Result(String nodeUrl, long nodeTime) {
            this.nodeUrl = nodeUrl;
            this.nodeTime = nodeTime;
        }

        public String getNodeUrl() {
            return nodeUrl;
        }

        public long getNodeTime() {
            return nodeTime;
        }

        @Override
        public String toString() {
            return "Result{" + "nodeUrl='" + nodeUrl + '\'' + ", nodeTime=" + nodeTime + '}';
        }
    }

}
