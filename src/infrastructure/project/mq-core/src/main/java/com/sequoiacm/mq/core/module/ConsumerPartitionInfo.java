package com.sequoiacm.mq.core.module;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.types.BasicBSONList;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.common.BsonUtils;

public class ConsumerPartitionInfo {
    public static final String FIELD_TOPIC = "topic";
    public static final String FIELD_CONSUMER_GROUP = "consumer_group";
    public static final String FIELD_PARTITION_NUM = "partition_num";
    public static final String FIELD_CONSUMER = "consumer";
    public static final String FIELD_LAST_DELEVERED_ID = "last_delevered_id";
    public static final String FIELD_PENDING_MSG = "pending_msg";
    public static final String FIELD_LAST_REQUEST_TIME = "last_request_time";

    @JsonProperty(FIELD_TOPIC)
    private String topic;
    @JsonProperty(FIELD_CONSUMER_GROUP)
    private String consumerGroup;

    @JsonProperty(FIELD_PARTITION_NUM)
    private int partitionNum;
    @JsonProperty(FIELD_CONSUMER)
    private String consumer;
    @JsonProperty(FIELD_LAST_DELEVERED_ID)
    private long lastDeliveredId;
    @JsonProperty(FIELD_PENDING_MSG)
    private List<Long> pendingMsgs;
    @JsonProperty(FIELD_LAST_REQUEST_TIME)
    private long lastRequestTime;

    public ConsumerPartitionInfo() {

    }

    public ConsumerPartitionInfo(int n, String consumer) {
        this.partitionNum = n;
        this.consumer = consumer;
    }

    public ConsumerPartitionInfo(BSONObject bson) {
        topic = BsonUtils.getStringChecked(bson, FIELD_TOPIC);
        consumerGroup = BsonUtils.getStringChecked(bson, FIELD_CONSUMER_GROUP);
        partitionNum = BsonUtils.getIntegerChecked(bson, FIELD_PARTITION_NUM);
        consumer = BsonUtils.getString(bson, FIELD_CONSUMER);
        lastDeliveredId = BsonUtils.getNumberChecked(bson, FIELD_LAST_DELEVERED_ID).longValue();
        pendingMsgs = new ArrayList<>();
        BasicBSONList pendingMsgBson = BsonUtils.getArray(bson, FIELD_PENDING_MSG);
        if (pendingMsgBson != null) {
            for (Object b : pendingMsgBson) {
                Number num = (Number) b;
                pendingMsgs.add(num.longValue());
            }
        }
        lastRequestTime = BsonUtils.getNumberChecked(bson, FIELD_LAST_REQUEST_TIME).longValue();
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public int getPartitionNum() {
        return partitionNum;
    }

    public void setPartitionNum(int partitionNum) {
        this.partitionNum = partitionNum;
    }

    public String getConsumer() {
        return consumer;
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    public long getLastDeliveredId() {
        return lastDeliveredId;
    }

    public void setLastDeliveredId(long lastDeliveredId) {
        this.lastDeliveredId = lastDeliveredId;
    }

    public List<Long> getPendingMsgs() {
        return pendingMsgs;
    }

    public void setPendingMsgs(List<Long> pendingMsgs) {
        this.pendingMsgs = pendingMsgs;
    }

    public long getLastRequestTime() {
        return lastRequestTime;
    }

    public void setLastRequestTime(long lastRequestTime) {
        this.lastRequestTime = lastRequestTime;
    }

    @Override
    public String toString() {
        return "ConsumerPartitionInfo [topic=" + topic + ", consumerGroup=" + consumerGroup
                + ", partitionNum=" + partitionNum + ", consumer=" + consumer + ", lastDeliveredId="
                + lastDeliveredId + ", pendingMsgs=" + pendingMsgs + ", lastRequestTime="
                + lastRequestTime + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((consumer == null) ? 0 : consumer.hashCode());
        result = prime * result + ((consumerGroup == null) ? 0 : consumerGroup.hashCode());
        result = prime * result + (int) (lastDeliveredId ^ (lastDeliveredId >>> 32));
        result = prime * result + (int) (lastRequestTime ^ (lastRequestTime >>> 32));
        result = prime * result + partitionNum;
        result = prime * result + ((pendingMsgs == null) ? 0 : pendingMsgs.hashCode());
        result = prime * result + ((topic == null) ? 0 : topic.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConsumerPartitionInfo other = (ConsumerPartitionInfo) obj;
        if (consumer == null) {
            if (other.consumer != null)
                return false;
        }
        else if (!consumer.equals(other.consumer))
            return false;
        if (consumerGroup == null) {
            if (other.consumerGroup != null)
                return false;
        }
        else if (!consumerGroup.equals(other.consumerGroup))
            return false;
        if (lastDeliveredId != other.lastDeliveredId)
            return false;
        if (lastRequestTime != other.lastRequestTime)
            return false;
        if (partitionNum != other.partitionNum)
            return false;
        if (pendingMsgs == null) {
            if (other.pendingMsgs != null)
                return false;
        }
        else if (!pendingMsgs.equals(other.pendingMsgs))
            return false;
        if (topic == null) {
            if (other.topic != null)
                return false;
        }
        else if (!topic.equals(other.topic))
            return false;
        return true;
    }

}
