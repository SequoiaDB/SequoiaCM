package com.sequoiacm.mq.server.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerPartitionInfo;

public class PartitionDistibutor {
    private Map<String, List<ConsumerPartitionInfo>> consumer2Partitions = new HashMap<>();
    private List<ConsumerPartitionInfo> allPartition;
    private List<ConsumerPartitionInfo> noConsumerPartition = new ArrayList<>();

    public PartitionDistibutor(List<ConsumerPartitionInfo> partitionInfos) {
        this.allPartition = partitionInfos;
        for (ConsumerPartitionInfo p : partitionInfos) {
            if (p.getConsumer() == null) {
                noConsumerPartition.add(p);
                continue;
            }
            List<ConsumerPartitionInfo> l = consumer2Partitions.get(p.getConsumer());
            if (l == null) {
                l = new ArrayList<>();
                consumer2Partitions.put(p.getConsumer(), l);
            }
            l.add(p);
        }
    }

    public boolean hasPartitionForNewConsumer() {
        if (consumer2Partitions.keySet().size() < allPartition.size()) {
            return true;
        }

        return false;
    }

    public Map<Integer, String> acquiresPartition(String newConsumer) throws MqException {
        if (!hasPartitionForNewConsumer()) {
            throw new MqException(MqError.NO_PARTITION_FOR_CONSUMER,
                    "no partition for new consumer");
        }
        if (consumer2Partitions.containsKey(newConsumer)) {
            return new HashMap<>();
        }
        consumer2Partitions.put(newConsumer, new ArrayList<ConsumerPartitionInfo>());
        return rebalance();
    }

    public Map<Integer, String> rleasePartition(String consumer) {
        Map<Integer, String> ret = new HashMap<>();

        List<ConsumerPartitionInfo> releasedPartitions = consumer2Partitions.remove(consumer);
        if (releasedPartitions == null) {
            return ret;
        }

        for (ConsumerPartitionInfo p : releasedPartitions) {
            p.setConsumer(null);
            noConsumerPartition.add(p);
            ret.put(p.getPartitionNum(), null);
        }
        releasedPartitions.clear();

        Map<Integer, String> reblanceRet = rebalance();
        ret.putAll(reblanceRet);
        return ret;
    }

    public Map<Integer, String> removePartition(List<Integer> removePartitionNums) {
        for (Iterator<ConsumerPartitionInfo> iterator = allPartition.iterator(); iterator
                .hasNext();) {
            ConsumerPartitionInfo p = iterator.next();
            if (!removePartitionNums.contains(p.getPartitionNum())) {
                continue;
            }
            iterator.remove();
            if (p.getConsumer() == null) {
                noConsumerPartition.remove(p);
                continue;
            }
            List<ConsumerPartitionInfo> partitions = consumer2Partitions.get(p.getConsumer());
            partitions.remove(p);
        }

        // 移除分区可能会导致分区数小于消费者数，此时需要踢掉多余的消费者
        if (allPartition.size() < consumer2Partitions.keySet().size()) {
            int surplusConsumer = consumer2Partitions.keySet().size() - allPartition.size();
            Iterator<Entry<String, List<ConsumerPartitionInfo>>> it = consumer2Partitions.entrySet()
                    .iterator();
            while (it.hasNext()) {
                Entry<String, List<ConsumerPartitionInfo>> entry = it.next();
                if (entry.getValue().size() <= 0) {
                    it.remove();
                    surplusConsumer--;
                    if (surplusConsumer <= 0) {
                        break;
                    }
                }
            }
        }
        return rebalance();
    }

    public Map<Integer, String> rebalance() {
        Map<Integer, String> ret = new HashMap<>();
        if (consumer2Partitions.keySet().size() == 0) {
            return ret;
        }

        int avg = allPartition.size() / consumer2Partitions.keySet().size();
        int remainder = allPartition.size() % consumer2Partitions.keySet().size();

        for (Entry<String, List<ConsumerPartitionInfo>> entry : consumer2Partitions.entrySet()) {
            List<ConsumerPartitionInfo> partitions = entry.getValue();
            String partitionConsumer = entry.getKey();
            if (partitions.size() >= avg) {
                continue;
            }
            int acquireCount = avg - partitions.size();
            List<ConsumerPartitionInfo> acPartitions = acquirePartitionFromNoConsumer(acquireCount);
            acquireCount = acquireCount - acPartitions.size();
            acPartitions.addAll(acquirePartitionFromOtherConsumer(avg, remainder, acquireCount));

            for (ConsumerPartitionInfo p : acPartitions) {
                p.setConsumer(partitionConsumer);
                partitions.add(p);
                ret.put(p.getPartitionNum(), partitionConsumer);
            }
        }
        Iterator<Entry<String, List<ConsumerPartitionInfo>>> cpIt = consumer2Partitions.entrySet()
                .iterator();
        Iterator<ConsumerPartitionInfo> ncIt = noConsumerPartition.iterator();
        while (cpIt.hasNext() && ncIt.hasNext()) {
            Entry<String, List<ConsumerPartitionInfo>> entry = cpIt.next();
            if (entry.getValue().size() > avg) {
                continue;
            }
            ConsumerPartitionInfo p = ncIt.next();
            p.setConsumer(entry.getKey());
            ret.put(p.getPartitionNum(), entry.getKey());
            entry.getValue().add(p);
        }
        noConsumerPartition.clear();
        return ret;
    }

    private List<ConsumerPartitionInfo> acquirePartitionFromNoConsumer(int acquireSize) {
        List<ConsumerPartitionInfo> ret = new ArrayList<>(acquireSize);
        while (noConsumerPartition.size() > 0 && ret.size() < acquireSize) {
            ret.add(noConsumerPartition.remove(noConsumerPartition.size() - 1));
        }
        return ret;
    }

    private List<ConsumerPartitionInfo> acquirePartitionFromOtherConsumer(int avg, int remainder,
            int acquireCount) {
        List<ConsumerPartitionInfo> ret = new ArrayList<>(acquireCount);
        if (acquireCount <= 0) {
            return ret;
        }

        List<List<ConsumerPartitionInfo>> partitionsPerConsumer = new ArrayList<>(
                consumer2Partitions.values());
        Collections.sort(partitionsPerConsumer, new Comparator<List<?>>() {
            @Override
            public int compare(List<?> o1, List<?> o2) {
                return o2.size() - o1.size();
            }
        });

        for (List<ConsumerPartitionInfo> partitions : partitionsPerConsumer) {
            if (partitions.size() <= avg) {
                break;
            }
            int surplus = partitions.size() - avg;

            // remainder 表示有几个消费者持有 avg + 1 个分区数
            if (remainder > 0) {
                remainder--;
                surplus = surplus - 1;
                if (surplus <= 0) {
                    continue;
                }
            }

            Iterator<ConsumerPartitionInfo> iterator = partitions.iterator();
            while (iterator.hasNext()) {
                ConsumerPartitionInfo p = iterator.next();
                if (p.getPendingMsgs() != null && p.getPendingMsgs().size() > 0) {
                    continue;
                }
                iterator.remove();
                ret.add(p);

                if (ret.size() >= acquireCount) {
                    return ret;
                }

                surplus--;
                if (surplus <= 0) {
                    break;
                }
            }
        }
        return ret;
    }

    public Map<Integer, String> addPartition(List<Integer> partitionNums) {
        for (Integer partitionNum : partitionNums) {
            ConsumerPartitionInfo p = new ConsumerPartitionInfo();
            p.setPartitionNum(partitionNum);
            p.setConsumer(null);
            noConsumerPartition.add(p);
            allPartition.add(p);
        }
        return rebalance();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, List<ConsumerPartitionInfo>> entry : consumer2Partitions.entrySet()) {
            sb.append("consumer: " + entry.getKey());
            sb.append(", partitions: ");
            for (ConsumerPartitionInfo p : entry.getValue()) {
                sb.append(p.getPartitionNum());
                sb.append("  ");
            }
            sb.append("\r\n");
        }

        sb.append("\r\n");
        sb.append("allPartition: ");
        for (ConsumerPartitionInfo p : allPartition) {
            sb.append(p.getPartitionNum() + ":" + p.getConsumer());
            sb.append("  ");
        }

        sb.append("\r\n");
        sb.append("noConsumerPartition: ");
        for (ConsumerPartitionInfo p : noConsumerPartition) {
            sb.append(p.getPartitionNum() + ":" + p.getConsumer());
            sb.append("  ");
        }

        return sb.toString();
    }
}
