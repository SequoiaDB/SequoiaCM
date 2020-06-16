package com.sequoiacm.mq.server.service;

import java.util.List;

import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.ConsumerPartitionInfo;

public interface PartitionService {
    public boolean acquiresPartition(String group, String consumer) throws MqException;

    public void releasePartition(String group, String consumer) throws MqException;

    public List<ConsumerPartitionInfo> getPartitionInfo(String groupName) throws MqException;

    public List<ConsumerPartitionInfo> getPartitionInfo(String groupName, String consumer)
            throws MqException;
}
