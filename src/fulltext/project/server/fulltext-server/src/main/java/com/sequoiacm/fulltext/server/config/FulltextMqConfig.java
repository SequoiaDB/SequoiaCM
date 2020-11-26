package com.sequoiacm.fulltext.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration @ConfigurationProperties("scm.fulltext.mq") public class FulltextMqConfig {
    private int topicPartitionNum = 3;

    public int getTopicPartitionNum() {
        return topicPartitionNum;
    }

    public void setTopicPartitionNum(int topicPartitionNum) {
        this.topicPartitionNum = topicPartitionNum;
    }
}
