package com.sequoiacm.mq.core.module;

import org.bson.BSONObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.common.BsonUtils;

public class Topic {
    public static final String FIELD_NAME = "name";
    public static final String FIELD_PARTITION_COUNT = "partition_count";
    public static final String FIELD_MESSAGE_TABLE_NAME = "message_table_name";

    @JsonProperty(value = FIELD_NAME)
    protected String name;
    @JsonProperty(value = FIELD_PARTITION_COUNT)
    protected int partitionCount;
    @JsonProperty(value = FIELD_MESSAGE_TABLE_NAME)
    protected String messageTableName;

    public Topic() {
    }

    public Topic(String name, int partitionCount, String messageTableName) {
        this.name = name;
        this.partitionCount = partitionCount;
        this.messageTableName = messageTableName;
    }

    public Topic(BSONObject bson) {
        name = BsonUtils.getStringChecked(bson, FIELD_NAME);
        messageTableName = BsonUtils.getStringChecked(bson, FIELD_MESSAGE_TABLE_NAME);
        partitionCount = BsonUtils.getIntegerChecked(bson, FIELD_PARTITION_COUNT);
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

    @Override
    public String toString() {
        return "Topic [name=" + name + ", partitionCount=" + partitionCount + ", messageTableName="
                + messageTableName + "]";
    }

}
