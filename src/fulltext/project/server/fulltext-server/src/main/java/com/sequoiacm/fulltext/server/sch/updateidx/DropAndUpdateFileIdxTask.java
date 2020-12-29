package com.sequoiacm.fulltext.server.sch.updateidx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.fulltext.server.fileidx.FileIdxDao;
import com.sequoiacm.fulltext.server.sch.IdxTaskContext;

public class DropAndUpdateFileIdxTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DropAndUpdateFileIdxTask.class);

    private IdxTaskContext context;

    private FileIdxDao dao;

    public DropAndUpdateFileIdxTask(FileIdxDao dao, IdxTaskContext context) {
        this.dao = dao;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            dao.process();
            if (context != null) {
                context.incSuccessCount(dao.processFileCount());
            }
        }
        catch (Exception e) {
            logger.error("failed to drop index for file:ws={}, fileId={}", dao.getWsName(),
                    dao.getFileId(), e);
            if (context != null) {
                context.incErrorCount(dao.processFileCount());
            }
        }
        finally {
            if (context != null) {
                context.reduceTaskCount();
            }
        }
    }
}
