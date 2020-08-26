package com.sequoiacm.fulltext.server.sch.updateidx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.fulltext.server.sch.IdxTaskContext;

public class IdxDropAndUpdateFileTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(IdxDropAndUpdateFileTask.class);

    private IdxTaskContext context;

    private IdxDropAndUpdateDao dao;

    public IdxDropAndUpdateFileTask(IdxDropAndUpdateDao dao, IdxTaskContext context) {
        this.dao = dao;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            dao.dropAndUpdate();
            if (context != null) {
                context.incSuccessCount(dao.getFileCount());
            }
        }
        catch (Exception e) {
            logger.error("failed to drop index for file:ws={}, fileId={}", dao.getWs(),
                    dao.getFileId(), e);
            if (context != null) {
                context.incErrorCount(dao.getFileCount());
            }
        }
        finally {
            if (context != null) {
                context.reduceTaskCount();
            }
        }
    }
}
