package com.sequoiacm.mq.server.service;

import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerPartitionInfo;
import com.sequoiacm.mq.core.module.MessageInternal;
import com.sequoiacm.mq.core.module.Topic;
import com.sequoiacm.mq.server.dao.*;
import com.sequoiacm.mq.server.lock.LockManager;
import com.sequoiacm.mq.server.lock.LockPathFactory;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 检查消息是否被消费的测试场景
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment =  SpringBootTest.WebEnvironment.NONE,classes = {
        MsgServiceImpl.class,
        TestsMsgRepository.class,
        TestsPartitionRepository.class,
        TestsTopicRepository.class
})
public class MsgServiceImplTestTwo {
    @Autowired
    private MsgRepository msgRepository;
    @MockBean
    private ConsumerGroupRepository consumerGroupRepository;
    @Autowired
    private PartitionRepository partitionRep;

    @Autowired
    private TopicRepository topicRepository;
    @MockBean
    private LockManager lockMgr;
    @MockBean
    private LockPathFactory lockPathFactory;

    @MockBean
    private ProducerAnConsumerClientMgr clientsMgr;
    @MockBean
    private MsgArriveNotifier msgArriveNotifier;

    @Autowired
    MsgService msgService;

    @Test
    public void checkMsgConsumedSituationTwo() throws MqException{
        /*
         * 测试场景：
         * 三个分区分别为0分区、1分区、2分区
         * 分区消息分配如下：
         * 0分区：1 2 8 10
         * 1分区：3 4 5 6 7 13
         * 2分区：9 11 12
         *
         * 分区消费情况如下：
         * p0->2
         * p1->7
         * p2->11
         *
         * 查询消息 1 2 3 6 7 8 10 11 12 13消息是否被消费
         * 查询消息 1 2 3 6 7 8 10 11 12 13消息及其之前的消息是否被消费
         *
         * 预期结果为：
         * true true true true true false false true false false
         * true true true true true false false false false false
         */
        Assert.assertTrue(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",1,false));
        Assert.assertTrue(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",2,false));
        Assert.assertTrue(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",3,false));
        Assert.assertTrue(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",6,false));
        Assert.assertTrue(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",7,false));
        Assert.assertFalse(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",8,false));
        Assert.assertFalse(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",10,false));
        Assert.assertTrue(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",11,false));
        Assert.assertFalse(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",12,false));
        Assert.assertFalse(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",13,false));

        Assert.assertTrue(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",1,true));
        Assert.assertTrue(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",2,true));
        Assert.assertTrue(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",3,true));
        Assert.assertTrue(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",6,true));
        Assert.assertTrue(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",7,true));
        Assert.assertFalse(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",8,true));
        Assert.assertFalse(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",10,true));
        Assert.assertFalse(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",11,true));
        Assert.assertFalse(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",12,true));
        Assert.assertFalse(msgService.checkMsgConsumed("sequoiadb_ws_FULLTEXT_FILE_OP","fulltext-server-sequoiadb_ws_FULLTEXT_FILE_OP",13,true));
    }
}

@Repository
class TestsTopicRepository implements TopicRepository{
    @Override
    public void createTopic(Transaction transaction, Topic topic) throws MqException {

    }

    @Override
    public void deleteTopic(Transaction transaction, String topicName) throws MqException {

    }

    @Override
    public Topic getTopic(String topicName) throws MqException {
        return new Topic("sequoiadb_ws_FULLTEXT_FILE_OP",3,"SCMSYSTEM.MQ_MSG_sequoiadb_ws_FULLTEXT_FILE_OP",0L);
    }

    @Override
    public List<Topic> getTopics() throws MqException {
        return null;
    }

    @Override
    public void updateTopic(Transaction transaction, String topicName, int newPartitionCount) throws MqException {

    }
}

@Repository
class TestsPartitionRepository implements PartitionRepository{
    private List<ConsumerPartitionInfo> consumerPartitionInfoList;

    public TestsPartitionRepository() {
        // 构建分区
        consumerPartitionInfoList = new ArrayList<>();
        for(int i = 0;i<3;i++){
            BSONObject o = new BasicBSONObject();
            o.put("topic","sequoiadb_ws_FULLTEXT_FILE_OP");
            o.put("consumer_group","fulltext-server-seuqoiadb_ws_FULLTEXT_FILE_OP");
            o.put("consumer","u1604-zsl:8510");
            if(i==0){
                o.put("partition_num",0);
                o.put("last_delevered_id",2L);
            }else if(i==2){
                o.put("partition_num",1);
                o.put("last_delevered_id",7L);
            }else{
                o.put("partition_num",2);
                o.put("last_delevered_id",11L);
            }
            o.put("pending_msg",new BasicBSONList());
            o.put("last_request_time",1609207392L+i);
            ConsumerPartitionInfo p = new ConsumerPartitionInfo(o);
            consumerPartitionInfoList.add(p);
        }
    }

    @Override
    public List<ConsumerPartitionInfo> getPartitionByTopicAndNum(String topicName, int partitionNum) throws MqException {
        return null;
    }

    @Override
    public void deletePartitionByTopic(Transaction t, String topic) throws MqException {

    }

    @Override
    public void deletePartitionByGroup(Transaction t, String group) throws MqException {
        consumerPartitionInfoList.remove(consumerPartitionInfoList.size()-1);
    }

    @Override
    public void deletePartition(Transaction t, String topic, int num) throws MqException {

    }

    @Override
    public void createPartition(Transaction t, String topic, String group, int num, long initLastDeleveredId, String consumer) throws MqException {

    }

    @Override
    public List<ConsumerPartitionInfo> getPartitionByGroup(String groupName) throws MqException {
        // 构造一些测试数据
        return consumerPartitionInfoList;
    }

    @Override
    public Map<Integer, List<ConsumerPartitionInfo>> getPartitionByTopic(String topicName) throws MqException {
        return null;
    }

    @Override
    public List<ConsumerPartitionInfo> getPartitions(String groupName, String consumer) throws MqException {
        return consumerPartitionInfoList;
    }

    @Override
    public void changePartitionConsumer(Transaction t, String groupName, int num, String newConsumer) throws MqException {

    }

    @Override
    public void changePartitionPendingMsg(Transaction t, String groupName, int num, List<Long> newPendingMsg) throws MqException {

    }

    @Override
    public void changePartitionLastDeleveredId(Transaction t, String groupName, int num, Long newLastDeleveredId) throws MqException {

    }

    @Override
    public void updatePartitionRequestTime(Transaction t, String groupName, int num) throws MqException {

    }

    @Override
    public void discardPartitionPendingMsg(Transaction t, String groupName, int num) throws MqException {

    }
}

@Repository
class TestsMsgRepository implements MsgRepository{
    // 构造测试用的message
    private List<MessageInternal> messageInternalList;

    public TestsMsgRepository() {
        // 构建消息
        messageInternalList = new ArrayList<>();
        for(int i = 1;i<=13;i++){
            BSONObject o = new BasicBSONObject();
            o.put("key","5fdc5c7f400001004d3dd244");
            o.put("id",i);
            o.put("topic","sequoiadb_ws_FULLTEXT_FILE_OP");
            if(i==1 || i==2 || i==8 || i==10){
                o.put("partition_num",0);
            }else if(i==9 || i==11 ||i==12){
                o.put("partition_num",2);
            }else{
                o.put("partition_num",1);
            }
            o.put("create_time",1609207392L+i);
            BSONObject obj = new BasicBSONObject();
            obj.put("file_id","5fe993b0400001004d3dd478");
            obj.put("index_location","sequoiacm-fulltext-4-673f32e6-af35-4de3-8bbb-716d344a07de");
            obj.put("option_type","CREATE_IDX");
            obj.put("ws_name","sequoiadb_ws");
            o.put("msg_content",obj);
            messageInternalList.add(new MessageInternal(o));
        }
    }

    @Override
    public MessageInternal getMsgById(String msgTable, long msgId) throws MqException {
        return null;
    }

    @Override
    public List<MessageInternal> getMsg(String msgTable, int partitionNum, long gtMsgId, int maxReturnCount) throws MqException {
        return null;
    }

    @Override
    public MessageInternal getMsg(String msgTable, long msgId) throws MqException {
        for(MessageInternal msg:messageInternalList){
            if(msg.getId()==msgId){
                return msg;
            }
        }
        return null;
    }

    @Override
    public long putMsg(String msgTableName, MessageInternal msg) throws MqException {
        return 0;
    }

    @Override
    public MessageInternal getMaxIdMsg(String msgTable, int partitionNum) throws MqException {
        return null;
    }

    @Override
    public TableCreateResult createMsgTable(String topicName) throws MqException {
        return null;
    }

    @Override
    public void dropMsgTableSilence(String msgTable) throws MqException {

    }

    @Override
    public void dropMsg(String msgTable, int partitionNum, long lessThanOrEqualsId) throws MqException {

    }

    @Override
    public long getMsgCount(String msgTable) throws MqException {
        return messageInternalList.size();
    }

    @Override
    public long getMsgCount(String msgTable, int partitionNum, long greaterThanId, long lessThanOrEqualsId) throws MqException {
        int ret = 0;
        for(MessageInternal m:messageInternalList){
            if(m.getPartition()==partitionNum && m.getId()>greaterThanId && m.getId()<=lessThanOrEqualsId){
                ret++;
            }
        }
        return ret;
    }
}
