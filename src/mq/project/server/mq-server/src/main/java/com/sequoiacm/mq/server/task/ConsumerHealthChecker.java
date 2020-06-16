package com.sequoiacm.mq.server.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sequoiacm.infrastructure.feign.ScmFeignClient;
import com.sequoiacm.infrastructure.lock.ScmLock;
import com.sequoiacm.mq.core.CommonDefine;
import com.sequoiacm.mq.core.exception.FeignExceptionConverter;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroup;
import com.sequoiacm.mq.core.module.ConsumerPartitionInfo;
import com.sequoiacm.mq.server.common.PartitionDistibutor;
import com.sequoiacm.mq.server.config.ConsumerHealthCheckerConfig;
import com.sequoiacm.mq.server.dao.PartitionRepository;
import com.sequoiacm.mq.server.dao.Transaction;
import com.sequoiacm.mq.server.dao.TransactionFactory;
import com.sequoiacm.mq.server.lock.LockManager;
import com.sequoiacm.mq.server.lock.LockPathFactory;
import com.sequoiacm.mq.server.service.ConsumerGroupService;

@Component
public class ConsumerHealthChecker extends BackgroundJob {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerHealthChecker.class);
    @Autowired
    private ScmFeignClient feign;

    @Autowired
    private PartitionRepository partitionRep;
    @Autowired
    private ConsumerGroupService groupService;
    @Autowired
    TransactionFactory transactionFactory;

    @Autowired
    private LockManager lockMgr;

    @Autowired
    private LockPathFactory lockPathFactory;

    @Autowired
    private ConsumerHealthCheckerConfig conf;

    private Map<String, ConsumerHealthFeignClient> clientCache = new ConcurrentHashMap<>();

    private Set<String> unreachableConsumer = new HashSet<>();

    @Override
    public void run() {
        try {
            doTask();
        }
        catch (Exception e) {
            logger.warn("failed to check consumer health", e);
        }
    }

    private boolean isHealth(String group, String consumer) {
        try {
            boolean isUp = getClient(consumer).health(group);
            unreachableConsumer.remove(consumer);
            return isUp;
        }
        catch (Exception e) {
            if (unreachableConsumer.contains(consumer)) {
                // 第二次连不上，认为这个消费者不健康
                unreachableConsumer.remove(consumer);
                logger.warn(
                        "after tow unexpected exception in quick succession, assume consumer is unhealth:group={}, consumer={}",
                        group, consumer, e);
                return false;
            }
            // 第一次连不上这个消费，加入集合，认为这个消费者还是健康的
            unreachableConsumer.add(consumer);
            logger.warn("failed to check consumer is health or not:group={}, consumer={}", group,
                    consumer, e);
            return true;
        }
    }

    private ConsumerHealthFeignClient getClient(String instanceAddr) {
        ConsumerHealthFeignClient client = clientCache.get(instanceAddr);
        if (client == null) {
            client = feign.builder().exceptionConverter(new FeignExceptionConverter())
                    .instanceTarget(ConsumerHealthFeignClient.class, instanceAddr);
            clientCache.put(instanceAddr, client);
        }
        return client;
    }

    private void doTask() throws MqException {
        List<ConsumerGroup> groups = groupService.getAllGroup();
        for (ConsumerGroup group : groups) {
            List<ConsumerPartitionInfo> partitions = partitionRep
                    .getPartitionByGroup(group.getName());
            List<String> unhealthConsumers = getUnhealthConsumer(partitions);
            if (unhealthConsumers.size() <= 0) {
                continue;
            }
            ScmLock pullMsgWriteLock = lockMgr
                    .acquiresWriteLock(lockPathFactory.pullMsgLockPath(group.getTopic()));
            try {
                partitions = partitionRep.getPartitionByGroup(group.getName());
                Map<Integer, String> modifier = new HashMap<>();
                PartitionDistibutor pd = new PartitionDistibutor(partitions);
                for (String unhealthConsumer : unhealthConsumers) {
                    modifier.putAll(pd.rleasePartition(unhealthConsumer));
                }
                if (modifier.size() <= 0) {
                    continue;
                }

                doModify(group, modifier);
            }
            finally {
                pullMsgWriteLock.unlock();
            }

        }
    }

    private void doModify(ConsumerGroup group, Map<Integer, String> modifier) throws MqException {
        Transaction transaction = transactionFactory.createTransaction();
        try {
            transaction.begin();
            for (Entry<Integer, String> entry : modifier.entrySet()) {
                partitionRep.changePartitionConsumer(transaction, group.getName(), entry.getKey(),
                        entry.getValue());
            }
            transaction.commit();
        }
        catch (Exception e) {
            transaction.rollback();
        }
    }

    public List<String> getUnhealthConsumer(List<ConsumerPartitionInfo> partitions) {
        long now = System.currentTimeMillis();
        List<String> checkedConsumer = new ArrayList<>();
        List<String> unhealthConsumer = new ArrayList<>();
        for (ConsumerPartitionInfo p : partitions) {
            if (p.getConsumer() == null) {
                continue;
            }
            if (checkedConsumer.contains(p.getConsumer())) {
                continue;
            }
            checkedConsumer.add(p.getConsumer());
            if (now - p.getLastRequestTime() < conf.getIdleThreshold()) {
                continue;
            }
            if (isHealth(p.getConsumerGroup(), p.getConsumer())) {
                continue;
            }
            unhealthConsumer.add(p.getConsumer());
        }
        return unhealthConsumer;
    }

    @Override
    public String getJobName() {
        return "Consumer-Health-Checker";
    }

    @Override
    public long getPeriod() {
        return conf.getPeriod();
    }

}

enum ConsumerStatus {
    UP,
    DOWN,
    UNREACHABLE;
}

@RequestMapping("/internal/v1")
interface ConsumerHealthFeignClient {
    @GetMapping("/msg_queue/client/is_up")
    public boolean health(@RequestParam(CommonDefine.REST_CONSUMER_GROUP) String group);
}
