package com.sequoiacm.mq.server.dao;

import java.util.List;

import com.sequoiacm.mq.core.exception.MqException;
import com.sequoiacm.mq.core.module.MessageInternal;

/**
 * 这张表不允许事务操作，主要是由于putMsg这个动作如果能回滚，
 * 在RU级别下，回滚前的这个窗口可能会被pullMsg看到，从而导致分区消费情况和实际消息表不一致
 */
public interface MsgRepository {
    public List<MessageInternal> getMsg(String msgTable, int partitionNum, long gtMsgId,
            int maxReturnCount) throws MqException;

    public MessageInternal getMsg(String msgTable, long msgId) throws MqException;

    public long putMsg(String msgTableName, MessageInternal msg) throws MqException;

    public MessageInternal getMaxIdMsg(String msgTable, int partitionNum) throws MqException;

    public TableCreateResult createMsgTable(String topicName) throws MqException;

    public void dropMsgTableSilence(String msgTable) throws MqException;

    public void dropMsg(String msgTable, int partitionNum, long lessThanOrEqualsId)
            throws MqException;

    public long getMsgCount(String msgTable) throws MqException;

    public long getMsgCount(String msgTable, int partitionNum, long greaterThanId,
            long lessThanOrEqualsId) throws MqException;

}
