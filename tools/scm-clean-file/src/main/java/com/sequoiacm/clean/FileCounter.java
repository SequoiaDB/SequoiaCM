package com.sequoiacm.clean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class FileCounter {
    private AtomicLong time1 = new AtomicLong();
    private AtomicLong time2 = new AtomicLong();
    private AtomicLong time3 = new AtomicLong();
    private AtomicLong count1 = new AtomicLong();

    private static final Logger logger = LoggerFactory.getLogger(FileCounter.class);
    private AtomicLong success = new AtomicLong();
    private AtomicLong failed = new AtomicLong();
    private AtomicLong skip = new AtomicLong();
    public long lastLogTime = 0;

    public AtomicLong getSuccess() {
        return success;
    }

    public AtomicLong getFailed() {
        return failed;
    }

    public AtomicLong getSkip() {
        return skip;
    }

    public synchronized void logCounter() {
        long now = System.currentTimeMillis();
        if (now - lastLogTime > 30000) {
            logger.info(toString());
            lastLogTime = now;
        }
    }

    public synchronized void logCounterAtFinish() {
        logger.info("clean finish: " + toString());
    }

    @Override
    public String toString() {
        return "FileCounter{" + "success=" + success + ", failed=" + failed + ", skip=" + skip
                + '}';
    }

    public AtomicLong getTime1() {
        return time1;
    }

    public AtomicLong getTime2() {
        return time2;
    }

    public AtomicLong getTime3() {
        return time3;
    }

    public AtomicLong getCount1() {
        return count1;
    }
}
