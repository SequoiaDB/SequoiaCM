package com.sequoiacm.mq.server.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.MessageInternal;
import com.sequoiacm.mq.server.dao.MsgRepository;
import com.sequoiacm.mq.server.dao.TableCreateResult;
import com.sequoiacm.mq.server.dao.impl.SdbTemplate.SequoiadbCollectionTemplate;
import com.sequoiadb.exception.BaseException;
import com.sequoiadb.exception.SDBError;

@Component
public class SdbMsgRepository implements MsgRepository {
    private static final Logger logger = LoggerFactory.getLogger(SdbMsgRepository.class);
    @Autowired
    private SdbTemplate sdbTemplate;

    @Autowired
    private SdbTopicRepository sdbTopicRep;

    @Override
    public TableCreateResult createMsgTable(String topicName) throws MqException {
        String tableName = SdbDaoCommonDefine.MQ_CS_NAME + "." + SdbDaoCommonDefine.MQ_MSG_CL_PREFIX
                + topicName;
        TableCreateResult ret = new TableCreateResult();
        ret.setTableName(tableName);
        try {
            sdbTemplate.createCollection(SdbDaoCommonDefine.MQ_CS_NAME,
                    SdbDaoCommonDefine.MQ_MSG_CL_PREFIX + topicName, null);
            BSONObject idxKey = new BasicBSONObject(MessageInternal.FIELD_ID, 1);
            sdbTemplate.createIndex(SdbDaoCommonDefine.MQ_CS_NAME,
                    SdbDaoCommonDefine.MQ_MSG_CL_PREFIX + topicName, "id_index", idxKey, true,
                    false);
            ret.setAlreadyExist(false);
            return ret;
        }
        catch (BaseException e) {
            if (e.getErrorCode() == SDBError.SDB_DMS_EXIST.getErrorCode()) {
                SequoiadbCollectionTemplate cl = sdbTemplate.collection(tableName);
                if (cl.findOne(null) != null) {
                    throw new MqException(MqError.METASOURCE_ERROR,
                            "failed to create topic message collection, collection alredy exist and not empty:topic="
                                    + topicName + ", table=" + tableName);
                }
                ret.setAlreadyExist(true);
                return ret;
            }
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to create topic message collection:topic=" + topicName + ", table="
                            + tableName,
                    e);
        }
    }

    @Override
    public void dropMsgTableSilence(String tableName) throws MqException {
        try {
            sdbTemplate.dropCollection(tableName);
        }
        catch (Exception e) {
            logger.warn("failed to drop topic message table:table={}", tableName, e);
        }
    }

    @Override
    public List<MessageInternal> getMsg(String msgTable, int partitionNum, long gtMsgId,
            int maxReturnCount) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(msgTable);
        BasicBSONObject matcher = new BasicBSONObject(MessageInternal.FIELD_PARTITION_NUM,
                partitionNum);
        matcher.put(MessageInternal.FIELD_ID, new BasicBSONObject("$gt", gtMsgId));
        BasicBSONObject orderBy = new BasicBSONObject(MessageInternal.FIELD_ID, 1);
        try {
            List<BSONObject> msgs = cl.find(matcher, orderBy, 0, maxReturnCount);
            List<MessageInternal> ret = new ArrayList<>();
            for (BSONObject m : msgs) {
                ret.add(new MessageInternal(m));
            }
            return ret;
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR, "failed to get msg from sdb:table="
                    + msgTable + ",partition=" + partitionNum + ", greaterThanMsgId=" + gtMsgId, e);
        }
    }

    @Override
    public long putMsg(String msgTableName, MessageInternal msg) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(msgTableName);
        BasicBSONObject msgBson = new BasicBSONObject();
        long msgId = sdbTopicRep.incAndGetLatestMsgId(msg.getTopic());
        msgBson.put(MessageInternal.FIELD_ID, msgId);
        msgBson.put(MessageInternal.FIELD_CREATE_TIME, msg.getCreateTime());
        msgBson.put(MessageInternal.FIELD_KEY, msg.getKey());
        msgBson.put(MessageInternal.FIELD_MSG_CONTENT, msg.getMsgContent());
        msgBson.put(MessageInternal.FIELD_PARTITION_NUM, msg.getPartition());
        msgBson.put(MessageInternal.FIELD_TOPIC, msg.getTopic());
        try {
            cl.insert(msgBson);
            return msgId;
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to insert msg to sdb:table=" + msgTableName + ",msg=" + msg, e);
        }
    }

    @Override
    public MessageInternal getMaxIdMsg(String msgTable, int partitionNum) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(msgTable);

        BSONObject matcher = new BasicBSONObject();
        if (partitionNum != -1) {
            matcher.put(MessageInternal.FIELD_PARTITION_NUM, partitionNum);
        }
        BSONObject orderby = new BasicBSONObject(MessageInternal.FIELD_ID, -1);
        try {
            BSONObject record = cl.findOne(matcher, orderby);
            if (record == null) {
                return null;
            }
            return new MessageInternal(record);
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to get latest msg from sdb:table=" + msgTable, e);
        }
    }

    @Override
    public void dropMsg(String msgTable, int partitionNum, long lessThanOrEqualsId)
            throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(msgTable);
        BSONObject matcher = new BasicBSONObject(MessageInternal.FIELD_PARTITION_NUM, partitionNum);
        matcher.put(MessageInternal.FIELD_ID, new BasicBSONObject("$lte", lessThanOrEqualsId));
        try {
            cl.delete(matcher);
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to delete msg in sdb:table=" + msgTable + ", matcher=" + matcher, e);
        }
    }

    @Override
    public long getMsgCount(String msgTable) throws MqException {
        SequoiadbCollectionTemplate cl = sdbTemplate.collection(msgTable);
        try {
            return cl.count(null);
        }
        catch (Exception e) {
            throw new MqException(MqError.METASOURCE_ERROR,
                    "failed to get msg count:table=" + msgTable, e);
        }
    }

}
