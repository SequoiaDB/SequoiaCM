package com.sequoiacm.mq.server.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroup;
import com.sequoiacm.mq.core.module.ConsumerPartitionInfo;
import com.sequoiacm.mq.core.module.Topic;
import com.sequoiacm.mq.server.config.MsgCleanerConfig;
import com.sequoiacm.mq.server.dao.MsgRepository;
import com.sequoiacm.mq.server.service.ConsumerGroupService;
import com.sequoiacm.mq.server.service.PartitionService;
import com.sequoiacm.mq.server.service.TopicService;

@Component
public class MsgCleaner extends BackgroundJob {
    private Logger logger = LoggerFactory.getLogger(MsgCleaner.class);
    @Autowired
    private ConsumerGroupService groupService;
    @Autowired
    private TopicService topicService;

    @Autowired
    private PartitionService partitionService;
    @Autowired
    private MsgRepository msgRep;
    @Autowired
    private MsgCleanerConfig conf;

    @Override
    public void run() {
        try {
            doTask();
        }
        catch (Exception e) {
            logger.warn("failed to clean msg", e);
        }
    }

    private void doTask() throws MqException {
        List<Topic> topics = topicService.getTopics();
        for (Topic topic : topics) {
            if (msgRep.getMsgCount(topic.getMessageTableName()) < conf.getMsgCountThreshold()) {
                continue;
            }
            Map<Integer, Long> partitionNum2MinLastDeleveredId = new HashMap<>();
            List<ConsumerGroup> groups = groupService.getGroupsByTopic(topic.getName());
            for (ConsumerGroup group : groups) {
                List<ConsumerPartitionInfo> partitions = partitionService
                        .getPartitionInfo(group.getName());
                for (ConsumerPartitionInfo p : partitions) {
                    Long minLastDeleveredId = partitionNum2MinLastDeleveredId
                            .get(p.getPartitionNum());
                    if (minLastDeleveredId == null || minLastDeleveredId > p.getLastDeliveredId()) {
                        partitionNum2MinLastDeleveredId.put(p.getPartitionNum(),
                                p.getLastDeliveredId());
                    }
                }
            }
            for (Entry<Integer, Long> entry : partitionNum2MinLastDeleveredId.entrySet()) {
                msgRep.dropMsg(topic.getMessageTableName(), entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public String getJobName() {
        return "Message-Cleanner";
    }

    @Override
    public long getPeriod() {
        return conf.getPeriod();
    }

}
