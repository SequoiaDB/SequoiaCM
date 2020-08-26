package com.sequoiacm.mq.core.module;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TopicDetail extends Topic {
    public static final String FIELD_CONSUMER_GROUP = "consumer_group";
    @JsonProperty(FIELD_CONSUMER_GROUP)
    private List<String> consumerGroup;

    public TopicDetail() {
        super();
    }

    public TopicDetail(Topic t, List<String> consumerGroup) {
        super(t.getName(), t.getPartitionCount(), t.getMessageTableName());
        this.consumerGroup = consumerGroup;
    }

    public List<String> getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(List<String> consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    @Override
    public String toString() {
        return "TopicDetail [consumerGroup=" + consumerGroup + ", name=" + name
                + ", partitionCount=" + partitionCount + ", messageTableName=" + messageTableName
                + "]";
    }

}
