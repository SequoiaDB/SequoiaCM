package com.sequoiacm.mq.server.dao.impl;

import static com.sequoiacm.mq.server.dao.impl.SdbDaoCommonDefine.*;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.sequoiacm.infrastructure.common.BsonUtils;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.Topic;
import com.sequoiacm.mq.server.dao.TopicRepository;
import com.sequoiacm.mq.server.dao.Transaction;
import com.sequoiacm.mq.server.dao.impl.SdbTemplate.SequoiadbCollectionTemplate;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

@Repository
public class SdbTopicRepository implements TopicRepository {
    private static final Logger logger = LoggerFactory.getLogger(SdbTopicRepository.class);
    private static final String FIELD_LATEST_MSG_ID = "latest_msg_id";
    @Autowired
    private SdbTemplate sdbTemplate;
    private BSONObject incMsgIdModifier;

    public SdbTopicRepository() {
        incMsgIdModifier = new BasicBSONObject();
        BSONObject incOne = new BasicBSONObject(FIELD_LATEST_MSG_ID, 1);
        incMsgIdModifier.put("$inc", incOne);
    }

    long incAndGetLatestMsgId(String topic) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME, MQ_TOPIC_CL_NAME);

        BasicBSONObject matcher = new BasicBSONObject();
        matcher.put(Topic.FIELD_NAME, topic);

        BSONObject topicBson = null;
        try {
            topicBson = cl.findOneAndModify(matcher, incMsgIdModifier, true);
        }
        catch (BaseException e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to generate message id:topic=" + topic, e);
        }
        if (topicBson == null) {
            throw new MqException(MqError.TOPIC_NOT_EXIST, "topic not exist:" + topic);
        }
        return BsonUtils.getNumberChecked(topicBson, FIELD_LATEST_MSG_ID).longValue();
    }

    @Override
    public void createTopic(Transaction transaction, Topic topic) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME, MQ_TOPIC_CL_NAME);
        BasicBSONObject record = new BasicBSONObject();
        record.put(Topic.FIELD_NAME, topic.getName());
        record.put(Topic.FIELD_PARTITION_COUNT, topic.getPartitionCount());
        record.put(Topic.FIELD_MESSAGE_TABLE_NAME, topic.getMessageTableName());
        record.put(FIELD_LATEST_MSG_ID, 0);

        try {
            cl.insert(record, (SdbTransaction) transaction);
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_IXM_DUP_KEY.getErrorCode()) {
                throw new MqException(MqError.TOPIC_EXIST,
                        "topic alredy exist:topic=" + topic.getName(), e);
            }
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to insert topic info to sdb:topic=" + topic.getName(), e);
        }
    }

    @Override
    public Topic getTopic(String topicName) throws MqException {
        try {
            SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME, MQ_TOPIC_CL_NAME);
            BSONObject record = cl.findOne(new BasicBSONObject(Topic.FIELD_NAME, topicName));
            if (record == null) {
                return null;
            }
            return new Topic(record);
        }
        catch (BaseException e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to query topic from sdb:topic=" + topicName, e);
        }
    }

    @Override
    public void updateTopic(Transaction transaction, String topicName, int newPartitionCount)
            throws MqException {
        try {
            SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME, MQ_TOPIC_CL_NAME);
            BasicBSONObject modifier = new BasicBSONObject(Topic.FIELD_PARTITION_COUNT,
                    newPartitionCount);
            cl.update(new BasicBSONObject(Topic.FIELD_NAME, topicName),
                    new BasicBSONObject("$set", modifier));
        }
        catch (BaseException e) {
            throw new MqException(MqError.METASOURCE_ERROR, "failed to update topic in sdb:topic="
                    + topicName + ", newPartitionCount=" + newPartitionCount, e);
        }
    }

    @Override
    public void deleteTopic(Transaction transaction, String topicName) throws MqException {
        try {
            SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME, MQ_TOPIC_CL_NAME);
            cl.delete(new BasicBSONObject(Topic.FIELD_NAME, topicName),
                    (SdbTransaction) transaction);
        }
        catch (BaseException e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to delete topic from sdb:topic=" + topicName, e);
        }
    }

    @Override
    public List<Topic> getTopics() throws MqException {
        try {
            SequoiadbCollectionTemplate cl = sdbTemplate.collection(MQ_CS_NAME, MQ_TOPIC_CL_NAME);
            List<BSONObject> records = cl.find(null);
            ArrayList<Topic> ret = new ArrayList<>(records.size());
            for (BSONObject r : records) {
                ret.add(new Topic(r));
            }
            return ret;
        }
        catch (BaseException e) {
            throw new MqException(MqError.METASOURCE_ERROR, "failed to query topic from sdb", e);
        }
    }

}
