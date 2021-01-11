package com.sequoiacm.mq.core.module;

import org.bson.BSONObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.common.BsonUtils;

public class Topic {
    public static final String FIELD_NAME = "name";
    public static final String FIELD_PARTITION_COUNT = "partition_count";
    public static final String FIELD_MESSAGE_TABLE_NAME = "message_table_name";
    public static final String FIELD_LATEST_MSG_ID = "latest_msg_id";

    @JsonProperty(value = FIELD_NAME)
    protected String name;
    @JsonProperty(value = FIELD_PARTITION_COUNT)
    protected int partitionCount;
    @JsonProperty(value = FIELD_MESSAGE_TABLE_NAME)
    protected String messageTableName;
    @JsonProperty(value = FIELD_LATEST_MSG_ID)
    protected long latestMsgId;

    public Topic() {
    }

    public Topic(String name, int partitionCount, String messageTableName, long latestMsgId) {
        this.name = name;
        this.partitionCount = partitionCount;
        this.messageTableName = messageTableName;
        this.latestMsgId = latestMsgId;
    }

    public Topic(BSONObject bson) {
        name = BsonUtils.getStringChecked(bson, FIELD_NAME);
        messageTableName = BsonUtils.getStringChecked(bson, FIELD_MESSAGE_TABLE_NAME);
        partitionCount = BsonUtils.getIntegerChecked(bson, FIELD_PARTITION_COUNT);
        latestMsgId = BsonUtils.getNumberChecked(bson,FIELD_LATEST_MSG_ID).longValue();
    }

    public String getMessageTableName() {
        return messageTableName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPartitionCount() {
        return partitionCount;
    }

    public void setPartitionCount(int partitionCount) {
        this.partitionCount = partitionCount;
    }

    public void setMessageTableName(String messageTableName) {
        this.messageTableName = messageTableName;
    }

    public long getLatestMsgId() {
        return latestMsgId;
    }

    @Override
    public String toString() {
        return "Topic [name=" + name + ", partitionCount=" + partitionCount + ", messageTableName="
                + messageTableName +", latestMsgId=" + latestMsgId + "]";
    }
}