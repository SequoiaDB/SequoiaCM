package com.sequoiacm.fulltext.server.sch.createidx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.fulltext.server.sch.IdxTaskContext;

public class IdxCreateTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(IdxCreateTask.class);
    private IdxCreateDao idxCreator;
    private IdxTaskContext context;

    public IdxCreateTask(IdxCreateDao idxCreator, IdxTaskContext context) {
        this.idxCreator = idxCreator;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            idxCreator.createIdx();
            if (context != null) {
                context.incSuccessCount(idxCreator.fileCount());
            }
        }
        catch (Exception e) {
            logger.error("failed to create index for file:ws={}, fileId={}", idxCreator.getWsName(),
                    idxCreator.getFileId(), e);
            if (context != null) {
                context.incErrorCount(idxCreator.fileCount());
            }
        }
        finally {
            if (context != null) {
                context.reduceTaskCount();
            }
        }
    }

}
