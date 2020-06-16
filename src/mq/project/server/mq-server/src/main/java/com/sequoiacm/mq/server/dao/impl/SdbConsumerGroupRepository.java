package com.sequoiacm.mq.server.dao.impl;

import static com.sequoiacm.mq.server.dao.impl.SdbDaoCommonDefine.*;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroup;
import com.sequoiacm.mq.server.dao.ConsumerGroupRepository;
import com.sequoiacm.mq.server.dao.Transaction;
import com.sequoiacm.mq.server.dao.impl.SdbTemplate.SequoiadbCollectionTemplate;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

@Repository
public class SdbConsumerGroupRepository implements ConsumerGroupRepository {
    @Autowired
    private SdbTemplate sdbTemplate;

    @Override
    public void deleteGroupByTopic(Transaction t, String topicName) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_GROUP_CL_NAME);
        try {
            cl.delete(new BasicBSONObject(ConsumerGroup.FIELD_TOPIC, topicName),
                    (SdbTransaction) t);
        }
        catch (BaseException e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to clear consumer group in sdb:topic=" + topicName, e);
        }
    }

    @Override
    public List<ConsumerGroup> getGroupByTopic(String topic) throws MqException {
        try {
            return queryGroup(new BasicBSONObject(ConsumerGroup.FIELD_TOPIC, topic));
        }
        catch (BaseException e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to query consumer group from sdb:topic=" + topic, e);
        }
    }

    private List<ConsumerGroup> queryGroup(BSONObject matcher) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_GROUP_CL_NAME);
        List<BSONObject> bsons = cl.find(matcher);
        ArrayList<ConsumerGroup> ret = new ArrayList<>(bsons.size());
        for (BSONObject b : bsons) {
            ret.add(new ConsumerGroup(b));
        }
        return ret;

    }

    @Override
    public void createGroup(Transaction t, String topicName, String groupName) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_GROUP_CL_NAME);
        BasicBSONObject record = new BasicBSONObject();
        record.append(ConsumerGroup.FIELD_NAME, groupName).append(ConsumerGroup.FIELD_TOPIC,
                topicName);
        try {
            cl.insert(record, (SdbTransaction) t);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                throw new MqException(MqError.CONSUMER_GROUP_EXIST,
                        "consumer group exist:group=" + groupName + ", topic=" + topicName, e);
            }
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to insert consumer group to sdb:group=" + groupName + ", topic="
                            + topicName,
                    e);
        }
    }

    @Override
    public void deleteGroupByName(Transaction t, String groupName) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_GROUP_CL_NAME);
        try {
            cl.delete(new BasicBSONObject(ConsumerGroup.FIELD_NAME, groupName), (SdbTransaction) t);
        }
        catch (BaseException e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to delete consumer group:name=" + groupName, e);
        }
    }

    @Override
    public ConsumerGroup getGroupByName(String groupName) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME,
                MQ_CONSUMER_GROUP_CL_NAME);
        try {
            BSONObject bson = cl.findOne(new BasicBSONObject(ConsumerGroup.FIELD_NAME, groupName));
            if (bson == null) {
                return null;
            }
            return new ConsumerGroup(bson);
        }
        catch (BaseException e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to query consumer group from sdb:name=" + groupName, e);
        }
    }

    @Override
    public List<ConsumerGroup> getAllGroup() throws MqException {
        try {
            return queryGroup(new BasicBSONObject());
        }
        catch (BaseException e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to query all consumer group from sdb", e);
        }
    }

}
