package com.sequoiacm.mq.core.module;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Message<T> {
    public static final String FIELD_KEY = "key";
    public static final String FIELD_ID = "id";
    public static final String FIELD_PARTITION_NUM = "partition_num";
    public static final String FIELD_TOPIC = "topic";
    public static final String FIELD_CREATE_TIME = "create_time";
    public static final String FIELD_MSG_CONTENT = "msg_content";
    public static final String FIELD_MSG_PRODUCER = "msg_producer";

    @JsonProperty(FIELD_KEY)
    protected String key;
    @JsonProperty(FIELD_ID)
    protected long id;
    @JsonProperty(FIELD_TOPIC)
    protected String topic;
    @JsonProperty(FIELD_PARTITION_NUM)
    protected int partition;
    @JsonProperty(FIELD_CREATE_TIME)
    protected long createTime;

    @JsonProperty(FIELD_MSG_CONTENT)
    protected T msgContent;

    @JsonProperty(FIELD_MSG_PRODUCER)
    protected String msgProducer;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public T getMsgContent() {
        return msgContent;
    }

    public void setMsgContent(T msgContent) {
        this.msgContent = msgContent;
    }

    public void setMsgProducer(String msgProducer) {
        this.msgProducer = msgProducer;
    }

    public String getMsgProducer() {
        return msgProducer;
    }

    @Override
    public String toString() {
        return "MessageBase [key=" + key + ", id=" + id + ", topic=" + topic + ", partition="
                + partition + ", createTime=" + createTime + ", msgContent=" + msgContent + "]";
    }

}
