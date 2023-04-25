package com.sequoiacm.contentserver.job;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.contentserver.config.PropertiesUtils;
import com.sequoiacm.contentserver.dao.FileTransferInterrupter;

public class TaskTransfileInterrupter implements FileTransferInterrupter {
    private static final Logger logger = LoggerFactory.getLogger(TaskTransfileInterrupter.class);
    private int totalIncreaseLen = 0;
    // write 10MB check once
    private int checkRunningFlagLength = PropertiesUtils.getTransferCheckLength();
    private ScmTaskFile task;

    public TaskTransfileInterrupter(ScmTaskFile task) {
        this.task = task;
    }

    @Override
    public boolean isInterrupted(int increaseLen) {
        totalIncreaseLen += increaseLen;

        if (totalIncreaseLen > checkRunningFlagLength) {
            try {
                int flag = ScmTaskFile.getTaskRunningFlag(task.getTaskId());
                if (CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL == flag) {
                    logger.info("task canceled:taskId={}", task.getTaskId());
                    return true;
                }
            }
            catch (Exception e) {
                //ignore exception
            }

            if (task.getMaxExecTime() > 0) {
                Date now = new Date();
                if (now.getTime() - task.getTaskStartTime() > task.getMaxExecTime()) {
                    logger.warn("task timeout:taskId={}", task.getTaskId());
                    return true;
                }
            }

            totalIncreaseLen = 0;
        }

        return false;
    }

    @Override
    public void resetLen() {
        totalIncreaseLen = 0;
    }
}
