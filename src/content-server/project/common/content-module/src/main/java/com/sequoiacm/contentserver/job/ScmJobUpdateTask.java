package com.sequoiacm.contentserver.job;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.common.ServiceDefine;

public class ScmJobUpdateTask extends ScmBackgroundJob {
    private static final Logger logger = LoggerFactory.getLogger(ScmJobUpdateTask.class);

    ReadWriteLock rwLock = new ReentrantReadWriteLock();
    Map<String, TaskUpdator> updatorMap = new HashMap<>();

    @Override
    public int getType() {
        return ServiceDefine.Job.JOB_TYPE_UPDATE_TASK_STATUS;
    }

    @Override
    public String getName() {
        return "JOB_TYPE_UPDATE_TASK_STATUS";
    }

    @Override
    public long getPeriod() {
        //5 minutes
        return 5 * 60 * 1000;
    }

    @Override
    public void _run() {
        Map<String, TaskUpdator> tmpMap = new HashMap<>();
        Lock r = rwLock.readLock();
        r.lock();
        try {
            tmpMap.putAll(updatorMap);
        }
        finally {
            r.unlock();
        }

        for (TaskUpdator updator : tmpMap.values()) {
            try {
                logger.debug("redo update:taskId=" + updator.getTaskId());
                updator.doUpdate();
                removeUpdator(updator.getTaskId());
            }
            catch (Exception e) {
                logger.error("updator task status failed:taskId=" + updator.getTaskId(), e);
            }
        }
    }

    private void removeUpdator(String taskId) {
        Lock w = rwLock.writeLock();
        w.lock();
        try {
            updatorMap.remove(taskId);
        }
        finally {
            w.unlock();
        }
    }

    public void addUpdator(TaskUpdator updator) {
        Lock w = rwLock.writeLock();
        w.lock();
        try {
            updatorMap.put(updator.getTaskId(), updator);
        }
        finally {
            w.unlock();
        }
    }

}
