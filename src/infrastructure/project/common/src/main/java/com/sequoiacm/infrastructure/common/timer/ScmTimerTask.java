package com.sequoiacm.infrastructure.common.timer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ScmTimerTask implements Runnable {
    private Logger logger = LoggerFactory.getLogger(ScmTimerTask.class);
    private CountDownLatch setedFuture = new CountDownLatch(1);
    private volatile Future<?> myFuture = null;

    public void cancel() {
        while (true) {
            try {
                setedFuture.await();
                break;
            }
            catch (Exception e) {
                if (myFuture != null) {
                    break;
                }
                logger.warn("failed to wait for task future, try again", e);
            }
        }
        myFuture.cancel(false);
    }

    final void setFuture(Future<?> myFuture) {
        if (this.myFuture != null) {
            throw new RuntimeException("task already scheduled");
        }
        this.myFuture = myFuture;
        setedFuture.countDown();
    }
}
