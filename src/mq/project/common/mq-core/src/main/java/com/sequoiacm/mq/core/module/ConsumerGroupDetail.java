package com.sequoiacm.mq.core.module;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author huangqiaohui
 *
 */
public class ConsumerGroupDetail extends ConsumerGroup {
    public static final String FIELD_PARTITION_INFO = "partition_info";

    @JsonProperty(FIELD_PARTITION_INFO)
    private List<ConsumerPartitionInfo> consumerPartitionInfos;

    public ConsumerGroupDetail() {
        super();
    }

    public ConsumerGroupDetail(String topic, String name,
            List<ConsumerPartitionInfo> consumerPartitionInfos) {
        super(name, topic);
        this.consumerPartitionInfos = consumerPartitionInfos;
    }

    public List<ConsumerPartitionInfo> getConsumerPartitionInfos() {
        return consumerPartitionInfos;
    }

    public void setConsumerPartitionInfos(List<ConsumerPartitionInfo> consumerPartitionInfos) {
        this.consumerPartitionInfos = consumerPartitionInfos;
    }

    @Override
    public String toString() {
        return "ConsumerGroupDetail [consumerPartitionInfos=" + consumerPartitionInfos + ", name="
                + name + ", topic=" + topic + "]";
    }

}
