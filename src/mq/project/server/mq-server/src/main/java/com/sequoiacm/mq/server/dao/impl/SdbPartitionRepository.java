package com.sequoiacm.mq.server.dao.impl;

import static com.sequoiacm.mq.server.dao.impl.SdbDaoCommonDefine.MQ_CONSUMER_PARTITION_INFO_CL_NAME;
import static com.sequoiacm.mq.server.dao.impl.SdbDaoCommonDefine.MQ_CS_NAME;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerPartitionInfo;
import com.sequoiacm.mq.server.dao.PartitionRepository;
import com.sequoiacm.mq.server.dao.Transaction;
import com.sequoiacm.mq.server.dao.impl.SdbTemplate.SequoiadbCollectionTemplate;

@Repository
public class SdbPartitionRepository implements PartitionRepository {
    @Autowired
    private SdbTemplate sdbTemplate;

    @Override
    public void deletePartitionByTopic(Transaction t, String topic) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_PARTITION_INFO_CL_NAME);
        try {
            cl.delete(new BasicBSONObject(ConsumerPartitionInfo.FIELD_TOPIC, topic),
                    (SdbTransaction) t);
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to remove consumer patition info from sdb:topic=" + topic, e);
        }

    }

    @Override
    public void createPartition(Transaction t, String topic, String group, int num,
            long initLastDeleveredId, String consumer) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_PARTITION_INFO_CL_NAME);
        BasicBSONObject record = new BasicBSONObject();
        record.append(ConsumerPartitionInfo.FIELD_CONSUMER, consumer);
        record.append(ConsumerPartitionInfo.FIELD_CONSUMER_GROUP, group);
        record.append(ConsumerPartitionInfo.FIELD_LAST_DELEVERED_ID, initLastDeleveredId);
        record.append(ConsumerPartitionInfo.FIELD_LAST_REQUEST_TIME, System.currentTimeMillis());
        record.append(ConsumerPartitionInfo.FIELD_PARTITION_NUM, num);
        record.append(ConsumerPartitionInfo.FIELD_PENDING_MSG, new ArrayList<String>());
        record.append(ConsumerPartitionInfo.FIELD_TOPIC, topic);
        try {
            cl.insert(record, (SdbTransaction) t);
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to remove consumer patition info from sdb:topic=" + topic, e);
        }
    }

    @Override
    public List<ConsumerPartitionInfo> getPartitionByGroup(String groupName) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_PARTITION_INFO_CL_NAME);
        try {
            List<BSONObject> records = cl.find(
                    new BasicBSONObject(ConsumerPartitionInfo.FIELD_CONSUMER_GROUP, groupName));
            List<ConsumerPartitionInfo> ret = new ArrayList<>(records.size());
            for (BSONObject r : records) {
                ret.add(new ConsumerPartitionInfo(r));
            }
            return ret;
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to get consumer patition info from sdb:group=" + groupName, e);
        }
    }

    private List<ConsumerPartitionInfo> queryPartitions(BSONObject matcher) {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_PARTITION_INFO_CL_NAME);
        List<BSONObject> records = cl.find(matcher);
        List<ConsumerPartitionInfo> ret = new ArrayList<>();
        for (BSONObject r : records) {
            ConsumerPartitionInfo p = new ConsumerPartitionInfo(r);
            ret.add(p);
        }
        return ret;
    }

    @Override
    public List<ConsumerPartitionInfo> getPartitionByTopicAndNum(String topicName, int partitionNum)
            throws MqException {
        try {
            BasicBSONObject matcher = new BasicBSONObject(ConsumerPartitionInfo.FIELD_TOPIC,
                    topicName);
            matcher.put(ConsumerPartitionInfo.FIELD_PARTITION_NUM, partitionNum);
            return queryPartitions(matcher);
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to get consumer partition info from sdb:topic=" + topicName
                            + ", partitionNum=" + partitionNum,
                    e);
        }
    }

    @Override
    public List<ConsumerPartitionInfo> getPartitions(String groupName, String consumer)
            throws MqException {
        try {
            BasicBSONObject matcher = new BasicBSONObject(
                    ConsumerPartitionInfo.FIELD_CONSUMER_GROUP, groupName);
            matcher.put(ConsumerPartitionInfo.FIELD_CONSUMER, consumer);
            return queryPartitions(matcher);
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to get consumer partition info from sdb:group=" + groupName
                            + ", consumer=" + consumer,
                    e);
        }
    }

    @Override
    public void deletePartitionByGroup(Transaction t, String group) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_PARTITION_INFO_CL_NAME);
        try {
            cl.delete(new BasicBSONObject(ConsumerPartitionInfo.FIELD_CONSUMER_GROUP, group),
                    (SdbTransaction) t);
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to remove consumer partition info from sdb:group=" + group, e);
        }
    }

    @Override
    public void changePartitionConsumer(Transaction t, String groupName, int num,
            String newConsumer) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_PARTITION_INFO_CL_NAME);
        try {
            BasicBSONObject matcher = new BasicBSONObject(
                    ConsumerPartitionInfo.FIELD_CONSUMER_GROUP, groupName);
            matcher.put(ConsumerPartitionInfo.FIELD_PARTITION_NUM, num);
            BasicBSONObject modifier = new BasicBSONObject();
            modifier.put(ConsumerPartitionInfo.FIELD_CONSUMER, newConsumer);
            modifier.put(ConsumerPartitionInfo.FIELD_LAST_REQUEST_TIME, System.currentTimeMillis());
            modifier.put(ConsumerPartitionInfo.FIELD_PENDING_MSG, new ArrayList<>());
            BasicBSONObject set = new BasicBSONObject("$set", modifier);
            cl.update(matcher, set, (SdbTransaction) t);
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to update consumer partition info in sdb:group=" + groupName
                            + ", partitionNum=" + num + ", newConsumer=" + newConsumer,
                    e);
        }
    }

    @Override
    public void changePartitionLastDeleveredId(Transaction t, String groupName, int num,
            Long newLastDeleveredId) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_PARTITION_INFO_CL_NAME);
        try {
            BasicBSONObject matcher = new BasicBSONObject(
                    ConsumerPartitionInfo.FIELD_CONSUMER_GROUP, groupName);
            matcher.put(ConsumerPartitionInfo.FIELD_PARTITION_NUM, num);
            BasicBSONObject modifier = new BasicBSONObject();
            modifier.put(ConsumerPartitionInfo.FIELD_LAST_REQUEST_TIME, System.currentTimeMillis());
            modifier.put(ConsumerPartitionInfo.FIELD_PENDING_MSG, new ArrayList<>());
            modifier.put(ConsumerPartitionInfo.FIELD_LAST_DELEVERED_ID, newLastDeleveredId);
            BasicBSONObject set = new BasicBSONObject("$set", modifier);
            cl.update(matcher, set, (SdbTransaction) t);
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to update consumer partition info in sdb:group=" + groupName
                            + ", partitionNum=" + num + ", newLastDeleveredId="
                            + newLastDeleveredId,
                    e);
        }
    }

    @Override
    public void discardPartitionPendingMsg(Transaction t, String groupName, int num)
            throws MqException {
        changePartitionPendingMsg(t, groupName, num, new ArrayList<Long>());
    }

    @Override
    public void changePartitionPendingMsg(Transaction t, String groupName, int num,
            List<Long> newPendingMsg) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_PARTITION_INFO_CL_NAME);
        try {
            BasicBSONObject matcher = new BasicBSONObject(
                    ConsumerPartitionInfo.FIELD_CONSUMER_GROUP, groupName);
            matcher.put(ConsumerPartitionInfo.FIELD_PARTITION_NUM, num);
            BasicBSONObject modifier = new BasicBSONObject();
            modifier.put(ConsumerPartitionInfo.FIELD_LAST_REQUEST_TIME, System.currentTimeMillis());
            modifier.put(ConsumerPartitionInfo.FIELD_PENDING_MSG, newPendingMsg);
            BasicBSONObject set = new BasicBSONObject("$set", modifier);
            cl.update(matcher, set, (SdbTransaction) t);
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to update consumer partition info in sdb:group=" + groupName
                            + ", partitionNum=" + num + ", newPendingMsg=" + newPendingMsg,
                    e);
        }
    }

    @Override
    public void updatePartitionRequestTime(Transaction t, String groupName, int num)
            throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_PARTITION_INFO_CL_NAME);
        try {
            BasicBSONObject matcher = new BasicBSONObject(
                    ConsumerPartitionInfo.FIELD_CONSUMER_GROUP, groupName);
            matcher.put(ConsumerPartitionInfo.FIELD_PARTITION_NUM, num);
            BasicBSONObject modifier = new BasicBSONObject();
            modifier.put(ConsumerPartitionInfo.FIELD_LAST_REQUEST_TIME, System.currentTimeMillis());
            BasicBSONObject set = new BasicBSONObject("$set", modifier);
            cl.update(matcher, set, (SdbTransaction) t);
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to update consumer patition lastRequestTime in sdb:group=" + groupName
                            + ", partitionNum=" + num,
                    e);
        }
    }

    @Override
    public Map<Integer, List<ConsumerPartitionInfo>> getPartitionByTopic(String topicName)
            throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_PARTITION_INFO_CL_NAME);
        try {
            List<BSONObject> records = cl
                    .find(new BasicBSONObject(ConsumerPartitionInfo.FIELD_TOPIC, topicName));
            Map<Integer, List<ConsumerPartitionInfo>> ret = new HashMap<>();
            for (BSONObject r : records) {
                ConsumerPartitionInfo p = new ConsumerPartitionInfo(r);
                List<ConsumerPartitionInfo> ps = ret.get(p.getPartitionNum());
                if (ps == null) {
                    ps = new ArrayList<>();
                    ret.put(p.getPartitionNum(), ps);
                }
                ps.add(p);
            }
            return ret;
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to get consumer patition info from sdb:topic=" + topicName, e);
        }
    }

    @Override
    public void deletePartition(Transaction t, String topic, int num) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_PARTITION_INFO_CL_NAME);
        try {
            BasicBSONObject condition = new BasicBSONObject(ConsumerPartitionInfo.FIELD_TOPIC,
                    topic);
            condition.put(ConsumerPartitionInfo.FIELD_PARTITION_NUM, num);
            cl.delete(condition, (SdbTransaction) t);
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to remove consumer patition info from sdb:topic=" + topic
                            + ", partitionNum=" + num,
                    e);
        }
    }

}
