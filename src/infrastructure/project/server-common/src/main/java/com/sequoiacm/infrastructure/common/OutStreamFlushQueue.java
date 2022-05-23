package com.sequoiacm.infrastructure.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class OutStreamFlushQueue {
    private static final Logger logger = LoggerFactory.getLogger(OutStreamFlushQueue.class);
    public final static long TWENTY_SECONDS = 20 * 1000;
    private static Lock lock = new ReentrantLock();
    private AtomicLong index = new AtomicLong(0);
    private static Map<Long, ServletOutputStream> outputStreamHashMap = new HashMap<>();
    private static final byte[] WHITE_BYTE = " ".getBytes();

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

    @Scheduled(initialDelay = 1000 * 10, fixedDelay = TWENTY_SECONDS)
    public void flushOutputStream() {
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
            } catch (Exception e) {
                logger.error("the outputStream is invalid.", e);
                remove(entry.getKey(), entry.getValue());
            }
        }
        logger.debug("scan end, outputStreamHashMap size:{}", outputStreamHashMap.size());
    }
}
