package com.sequoiacm.infrastructure.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PreDestroy;
import javax.servlet.ServletOutputStream;

import com.sequoiacm.infrastructure.common.timer.ScmTimer;
import com.sequoiacm.infrastructure.common.timer.ScmTimerFactory;
import com.sequoiacm.infrastructure.common.timer.ScmTimerTask;
import com.sequoiacm.infrastructure.config.RequestKeepAliveConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestKeepAlive {
    private static final Logger logger = LoggerFactory.getLogger(RequestKeepAlive.class);
    private static Lock lock = new ReentrantLock();
    private AtomicLong index = new AtomicLong(0);
    private static Map<Long, ServletOutputStream> outputStreamHashMap = new HashMap<>();
    private static final byte[] WHITE_BYTE = " ".getBytes();

    private ScmTimer flushTimer;

    public RequestKeepAlive(RequestKeepAliveConfig requestKeepAliveConfig) {
        flushTimer = ScmTimerFactory.createScmTimer("OutputStreamKeepAlive");
        flushTimer.schedule(new ScmTimerTask() {
            @Override
            public void run() {
                flushOutputStream();
            }
        }, requestKeepAliveConfig.getInterval(), requestKeepAliveConfig.getInterval());
    }

    @PreDestroy
    public void destroy() {
        if (flushTimer != null) {
            flushTimer.cancel();
        }
    }

    public long add(ServletOutputStream outputStream) {
        lock.lock();
        try {
            long newIndex = this.index.getAndIncrement();
            outputStreamHashMap.put(newIndex, outputStream);
            return newIndex;
        }
        finally {
            lock.unlock();
        }
    }

    public void remove(Long flushIndex, ServletOutputStream outputStream) {
        if (flushIndex == null) {
            return;
        }
        lock.lock();
        try {
            ServletOutputStream mapOutputStream = outputStreamHashMap.get(flushIndex);
            if (mapOutputStream != null && mapOutputStream == outputStream) {
                outputStreamHashMap.remove(flushIndex);
            }
        }
        finally {
            lock.unlock();
        }
    }

    private void flushOutputStream() {
        logger.debug("scan begin, outputStreamHashMap size:{}", outputStreamHashMap.size());
        Set<Map.Entry<Long, ServletOutputStream>> duplicate = null;
        lock.lock();
        try {
            duplicate = new HashSet<>(outputStreamHashMap.entrySet());
        }
        finally {
            lock.unlock();
        }
        for (Map.Entry<Long, ServletOutputStream> entry : duplicate) {
            try {
                if (outputStreamHashMap.containsKey(entry.getKey())) {
                    entry.getValue().write(WHITE_BYTE);
                    entry.getValue().flush();
                }
            }
            catch (Exception e) {
                logger.error("the outputStream is invalid.", e);
                remove(entry.getKey(), entry.getValue());
            }
        }
        logger.debug("scan end, outputStreamHashMap size:{}", outputStreamHashMap.size());
    }

}
