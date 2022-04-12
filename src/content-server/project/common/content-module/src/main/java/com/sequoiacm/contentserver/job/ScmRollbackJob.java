package com.sequoiacm.contentserver.job;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.contentserver.common.ServiceDefine;
import com.sequoiacm.contentserver.transaction.ScmTransBase;
import com.sequoiacm.contentserver.transaction.ScmTransFactory;

class ScmRollbackInfo {
    private int siteID;
    private String workspaceName;
    private String transID;

    public ScmRollbackInfo(int siteID, String workspaceName, String transID) {
        this.siteID = siteID;
        this.workspaceName = workspaceName;
        this.transID = transID;
    }

    public int getSiteID() {
        return siteID;
    }

    public void setSiteID(int siteID) {
        this.siteID = siteID;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getTransID() {
        return transID;
    }

    public void setTransID(String transID) {
        this.transID = transID;
    }
}

public class ScmRollbackJob extends ScmBackgroundJob {
    private static final Logger logger = LoggerFactory.getLogger(ScmRollbackJob.class);

    private ReadWriteLock mapLock = new ReentrantReadWriteLock();
    private Map<String, ScmRollbackInfo> transIDMap = new LinkedHashMap<>();

    public ScmRollbackJob() {
    }

    public void addTransID(int siteID, String workspaceName, String transID) {
        Lock w = mapLock.writeLock();
        w.lock();
        try {
            transIDMap.put(transID, new ScmRollbackInfo(siteID, workspaceName, transID));
        }
        finally {
            w.unlock();
        }
    }

    private void copyAndClearSetter(Map<String, ScmRollbackInfo> destMap) {
        Lock w = mapLock.writeLock();
        w.lock();
        try {
            for (Map.Entry<String, ScmRollbackInfo> entry : transIDMap.entrySet()) {
                destMap.put(entry.getKey(), entry.getValue());
            }

            transIDMap.clear();
        }
        finally {
            w.unlock();
        }
    }

    @Override
    public void _run() {
        Map<String, ScmRollbackInfo> ids = new LinkedHashMap<>();
        copyAndClearSetter(ids);
        for (Map.Entry<String, ScmRollbackInfo> entry : ids.entrySet()) {
            logger.info("rollbacking transID:" + entry.getKey());
            ScmRollbackInfo info = entry.getValue();
            try {
                ScmTransBase stb = ScmTransFactory.getTransaction(info.getSiteID(),
                        info.getWorkspaceName(), info.getTransID());
                stb.rollback();
            }
            catch (Exception e) {
                logger.error("get transaction failed:siteID=" + info.getSiteID() + ",workspace=" +
                        info.getWorkspaceName() + ",transID=" + info.getTransID(), e);
                addTransID(info.getSiteID(), info.getWorkspaceName(), info.getTransID());
            }
        }
    }

    @Override
    public int getType() {
        return ServiceDefine.Job.JOB_TYPE_TRANS_ROLLBACK;
    }

    @Override
    public String getName() {
        return "rollback_job";
    }

    @Override
    public long getPeriod() {
        // delay as period
        return ServiceDefine.Job.TRANS_ROLLBACK_TASK_DELAY;
    }
}
