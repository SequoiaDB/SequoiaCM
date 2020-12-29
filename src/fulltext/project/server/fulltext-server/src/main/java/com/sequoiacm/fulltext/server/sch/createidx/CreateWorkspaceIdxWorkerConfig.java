package com.sequoiacm.fulltext.server.sch.createidx;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("scm.fulltext.workspaceIdxWorker")
public class CreateWorkspaceIdxWorkerConfig {
    // 存量建立索引时，多少个文件作为一个消息进行投递
    private int filesInOneMsg = 5;
    // 存量数据建立索引时，投递多少消息后检查下消费情况
    private int msgCountToCheckConsumed = 5000;

    public int getFilesInOneMsg() {
        return filesInOneMsg;
    }

    public void setFilesInOneMsg(int filesInOneMsg) {
        this.filesInOneMsg = filesInOneMsg;
    }

    public int getMsgCountToCheckConsumed() {
        return msgCountToCheckConsumed;
    }

    public void setMsgCountToCheckConsumed(int msgCountToCheckConsumed) {
        this.msgCountToCheckConsumed = msgCountToCheckConsumed;
    }
}
