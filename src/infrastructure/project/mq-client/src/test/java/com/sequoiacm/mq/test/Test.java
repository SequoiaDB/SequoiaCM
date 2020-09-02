//package com.sequoiacm.mq.test;
//
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.List;
//
//import org.bson.BSONObject;
//import org.bson.BasicBSONObject;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
//import org.springframework.util.Assert;
//
//import com.sequoiacm.mq.client.EnableScmMqAdmin;
//import com.sequoiacm.mq.client.EnableScmMqConsumer;
//import com.sequoiacm.mq.client.EnableScmMqProducer;
//import com.sequoiacm.mq.client.config.AdminClient;
//import com.sequoiacm.mq.client.core.ConsumerClient;
//import com.sequoiacm.mq.client.core.ConsumerClientMgr;
//import com.sequoiacm.mq.client.core.MessageDeseserializer;
//import com.sequoiacm.mq.client.core.MessageSerializer;
//import com.sequoiacm.mq.client.core.ProducerClient;
//import com.sequoiacm.mq.client.core.ProducerClientMgr;
//import com.sequoiacm.mq.core.exception.MqError;
//import com.sequoiacm.mq.core.exception.MqException;
//import com.sequoiacm.mq.core.module.ConsumerGroupDetail;
//import com.sequoiacm.mq.core.module.Message;
//import com.sequoiacm.mq.core.module.TopicDetail;
//
//@EnableScmMqAdmin
//@EnableScmMqConsumer
//@EnableScmMqProducer
//@EnableDiscoveryClient
//@SpringBootApplication
//public class Test implements ApplicationRunner {
//    private static final Logger logger = LoggerFactory.getLogger(Test.class);
//    @Autowired
//    private ConsumerClientMgr consumerMgr;
//    @Autowired
//    private ProducerClientMgr producerMgr;
//    @Autowired
//    private AdminClient adminClient;
//
//    public static void main(String[] args) {
//        SpringApplication.run(Test.class, args);
//    }
//
//    public void testTopic() throws MqException {
//
//        adminClient.deleteTopic("topic1");
//        adminClient.deleteTopic("topic2");
//        adminClient.createTopic("topic1", 3);
//        adminClient.createTopic("topic2", 4);
//        try {
//            adminClient.createTopic("topic2", 4);
//            Assert.isTrue(false, "");
//        }
//        catch (MqException e) {
//            Assert.isTrue(e.getError() == MqError.TOPIC_EXIST, "");
//        }
//
//        adminClient.createGroup("group1", "topic1");
//        TopicDetail topic = adminClient.getTopic("topic1");
//
//        Assert.isTrue(topic.getConsumerGroup().get(0).equals("group1"), "");
//        Assert.isTrue(topic.getName().equals("topic1"), "name!=topic1");
//        Assert.isTrue(topic.getPartitionCount() == 3, "partition!=3");
//
//        List<TopicDetail> ts = adminClient.listTopic();
//        Collections.sort(ts, new Comparator<TopicDetail>() {
//
//            @Override
//            public int compare(TopicDetail o1, TopicDetail o2) {
//                return o1.getName().compareTo(o2.getName());
//            }
//        });
//
//        Assert.isTrue(ts.size() == 2, ts.size() + "");
//        Assert.isTrue(ts.get(0).getName().equals("topic1"), "");
//        Assert.isTrue(ts.get(0).getConsumerGroup().get(0).equals("group1"), "");
//        Assert.isTrue(ts.get(0).getPartitionCount() == 3, "");
//
//        Assert.isTrue(ts.get(1).getName().equals("topic2"), "");
//        Assert.isTrue(ts.get(1).getConsumerGroup().size() == 0, "");
//        Assert.isTrue(ts.get(1).getPartitionCount() == 4, "");
//
//        adminClient.deleteTopic("topic1");
//        adminClient.deleteTopic("topic2");
//        try {
//            adminClient.getTopic("topic1");
//            Assert.isTrue(false, "");
//        }
//        catch (MqException e) {
//            Assert.isTrue(e.getError() == MqError.TOPIC_NOT_EXIST, "");
//        }
//        ts = adminClient.listTopic();
//        Assert.isTrue(ts.size() == 0, ts.size() + "");
//    }
//
//    @Override
//    public void run(ApplicationArguments args) throws Exception {
//        testTopic();
//        testGroup();
//        testMsg();
//        logger.info("test success!");
//        System.exit(0);
//        //        adminClient.createTopicIfNotExist("topic4", 4);
//        //        adminClient.createGroupIfNotExist("group4", "topic4", ConsumePosition.OLDEST);
//        //        List<ConsumerGroupDetail> g = adminClient.listGroup();
//        //        List<TopicDetail> t = adminClient.listTopic();
//        //        logger.info("topics:{}", t);
//        //        logger.info("groups:{}", g);
//        //        ConsumerClient<BSONObject> consumer = consumerMgr.createClient("group4",
//        //                new MessageDeseserializer<BSONObject>() {
//        //
//        //                    @Override
//        //                    public BSONObject deserialize(BSONObject m) {
//        //                        return m;
//        //                    }
//        //                });
//        //
//        //        while (true) {
//        //            List<Message<BSONObject>> msgs = consumer.pullMsg(2, 3);
//        //            if (msgs != null) {
//        //                boolean isBreak = false;
//        //                for (Message<BSONObject> m : msgs) {
//        //                    logger.info("get msg:" + m);
//        //                    if (m.getKey().equals("end222")) {
//        //                        isBreak = true;
//        //                        break;
//        //                    }
//        //                }
//        //                if (isBreak) {
//        //                    break;
//        //                }
//        //            }
//        //            else {
//        //                logger.info("no msg");
//        //            }
//        //        }
//        //        consumer.close();
//    }
//
//    private void testMsg() throws MqException {
//        adminClient.deleteTopic("topic1");
//        adminClient.createTopic("topic1", 1);
//        adminClient.createGroup("group1", "topic1");
//        ConsumerClient<BSONObject> consumer = consumerMgr.createClient("group1",
//                new MessageDeseserializer<BSONObject>() {
//
//                    @Override
//                    public BSONObject deserialize(BSONObject m) {
//                        return m;
//                    }
//                });
//        ProducerClient<BSONObject> producer = producerMgr
//                .createClient(new MessageSerializer<BSONObject>() {
//
//                    @Override
//                    public BSONObject serialize(BSONObject message) {
//                        return message;
//                    }
//                });
//
//        BasicBSONObject b = new BasicBSONObject("key", "value");
//        producer.putMsg("topic1", "k1", b);
//        List<Message<BSONObject>> m = consumer.pullMsg(1, 0);
//        Assert.isTrue(m.get(0).getKey().equals("k1"), "");
//        Assert.isTrue(m.get(0).getMsgContent().equals(b),
//                "expect:" + m.get(0).getMsgContent() + ", src=" + b);
//
//        producer.putMsg("topic1", "k2", b);
//        producer.putMsg("topic1", "k3", b);
//        producer.putMsg("topic1", "k4", b);
//        producer.putMsg("topic1", "k5", b);
//        m = consumer.pullMsg(3, 0);
//        Assert.isTrue(m.get(0).getKey().equals("k2"), "");
//        Assert.isTrue(m.get(0).getMsgContent().equals(b), "");
//        Assert.isTrue(m.get(1).getKey().equals("k3"), "");
//        Assert.isTrue(m.get(1).getMsgContent().equals(b), "");
//        Assert.isTrue(m.get(2).getKey().equals("k4"), "");
//        Assert.isTrue(m.get(2).getMsgContent().equals(b), "");
//        consumer.close();
//        consumer = consumerMgr.createClient("group1", new MessageDeseserializer<BSONObject>() {
//
//            @Override
//            public BSONObject deserialize(BSONObject m) {
//                return m;
//            }
//        });
//        m = consumer.pullMsg(1, 0);
//        Assert.isTrue(m.get(0).getKey().equals("k5"), "");
//        Assert.isTrue(m.get(0).getMsgContent().equals(b), "");
//        consumer.close();
//        adminClient.deleteTopic("topic1");
//    }
//
//    private void testGroup() throws MqException {
//        adminClient.deleteTopic("topic1");
//        adminClient.createTopic("topic1", 4);
//        adminClient.createGroup("group1", "topic1");
//        adminClient.createGroup("group2", "topic1");
//        try {
//            adminClient.createGroup("group1", "topic1");
//            Assert.isTrue(false, "");
//        }
//        catch (MqException e) {
//            Assert.isTrue(e.getError() == MqError.CONSUMER_GROUP_EXIST, "");
//        }
//
//        ConsumerGroupDetail g = adminClient.getGroup("group1");
//        Assert.isTrue(g.getName().equals("group1"), "");
//        Assert.isTrue(g.getConsumerPartitionInfos().size() == 4, "");
//        Assert.isTrue(g.getTopic().equals("topic1"), "");
//
//        List<ConsumerGroupDetail> gs = adminClient.listGroup();
//        Assert.isTrue(gs.size() == 2, "");
//
//        adminClient.deleteGroup("group1");
//        try {
//            g = adminClient.getGroup("group1");
//        }
//        catch (MqException e) {
//            Assert.isTrue(e.getError() == MqError.CONSUMER_GROUP_NOT_EXIST, "");
//        }
//
//        adminClient.deleteTopic("topic1");
//
//    }
//}
