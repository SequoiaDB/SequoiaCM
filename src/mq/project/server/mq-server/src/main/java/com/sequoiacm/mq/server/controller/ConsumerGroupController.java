package com.sequoiacm.mq.server.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequoiacm.mq.core.CommonDefine;
import com.sequoiacm.mq.core.exception.MqError;
import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerGroupOffsetEnum;
import com.sequoiacm.mq.core.module.ConsumerGroup;
import com.sequoiacm.mq.core.module.ConsumerGroupDetail;
import com.sequoiacm.mq.core.module.ConsumerPartitionInfo;
import com.sequoiacm.mq.server.service.ConsumerGroupService;
import com.sequoiacm.mq.server.service.PartitionService;

@RequestMapping("/internal/v1")
@RestController
public class ConsumerGroupController {
    @Autowired
    private ConsumerGroupService consumerGroupService;
    @Autowired
    private PartitionService partitionService;

    @PostMapping(value = "/msg_queue/consumer_groups")
    public void createGroup(@RequestParam(CommonDefine.REST_TOPIC) String topic,
            @RequestParam(CommonDefine.REST_NAME) String groupName,
            @RequestParam(value = CommonDefine.REST_OFFSET, required = false) ConsumerGroupOffsetEnum consumePosition)
            throws MqException {
        if (consumePosition == null) {
            consumePosition = ConsumerGroupOffsetEnum.OLDEST;
        }
        consumerGroupService.createGroup(topic, groupName, consumePosition);
    }

    @GetMapping(value = "/msg_queue/consumer_groups/{consumer_group:.+}")
    public ConsumerGroupDetail getGroup(@PathVariable("consumer_group") String groupName)
            throws MqException {
        ConsumerGroup group = consumerGroupService.getGroupByName(groupName);
        if (group == null) {
            throw new MqException(MqError.CONSUMER_GROUP_NOT_EXIST,
                    "group not exist:groupName=" + groupName);
        }
        List<ConsumerPartitionInfo> consumerPartitonInfos = partitionService
                .getPartitionInfo(groupName);
        return new ConsumerGroupDetail(group.getTopic(), group.getName(), consumerPartitonInfos);
    }

    @DeleteMapping(value = "/msg_queue/consumer_groups/{consumer_group:.+}")
    public void deleteGroup(@PathVariable("consumer_group") String groupName) throws MqException {
        consumerGroupService.deleteGroup(groupName);
    }

    @GetMapping(value = "/msg_queue/consumer_groups")
    public List<ConsumerGroupDetail> getGroups() throws MqException {
        List<ConsumerGroup> groups = consumerGroupService.getAllGroup();
        List<ConsumerGroupDetail> ret = new ArrayList<>(groups.size());
        for (ConsumerGroup group : groups) {
            List<ConsumerPartitionInfo> consumerPartitonInfos = partitionService
                    .getPartitionInfo(group.getName());
            ret.add(new ConsumerGroupDetail(group.getTopic(), group.getName(),
                    consumerPartitonInfos));
        }
        return ret;
    }

}
