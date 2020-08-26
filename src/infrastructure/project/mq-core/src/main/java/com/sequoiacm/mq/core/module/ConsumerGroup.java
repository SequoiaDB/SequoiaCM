package com.sequoiacm.mq.core.module;

import org.bson.BSONObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequoiacm.infrastructure.common.BsonUtils;

public class ConsumerGroup {
    public static final String FIELD_NAME = "name";
    public static final String FIELD_TOPIC = "topic";

    @JsonProperty(FIELD_NAME)
    protected String name;
    @JsonProperty(FIELD_TOPIC)
    protected String topic;

    public ConsumerGroup(String name, String topic) {
        this.name = name;
        this.topic = topic;
    }

    public ConsumerGroup() {
    }

    public ConsumerGroup(BSONObject bson) {
        name = BsonUtils.getStringChecked(bson, FIELD_NAME);
        topic = BsonUtils.getStringChecked(bson, FIELD_TOPIC);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public String toString() {
        return "ConsumerGroup [name=" + name + ", topic=" + topic + "]";
    }

}
