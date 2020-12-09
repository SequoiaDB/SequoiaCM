package com.sequoiacm.mq.server.lock;

import org.springframework.stereotype.Component;

@Component
public class LockPathFactory {
    private static final String MSG_QUEUE = "msg_queue";
    private static final String PULL_MSG = "pull_msg";
    private static final String PUT_MSG = "put_msg";
    private static final String GEN_MSG_ID_AND_INSERT_MSG = "gen_msg_id_and_insert_msg";

    public LockPath pullMsgLockPath(String topic) {
        String[] lockPath = { MSG_QUEUE, topic, PULL_MSG };
        return new LockPath(lockPath);
    }

    public LockPath putMsgLockPath(String topic) {
        String[] lockPath = { MSG_QUEUE, topic, PUT_MSG };
        return new LockPath(lockPath);
    }

    public LockPath genMsgIdAndInsertMsgLockPath(String topic) {
        String[] lockPath = { MSG_QUEUE, topic, GEN_MSG_ID_AND_INSERT_MSG };
        return new LockPath(lockPath);
    }

}
