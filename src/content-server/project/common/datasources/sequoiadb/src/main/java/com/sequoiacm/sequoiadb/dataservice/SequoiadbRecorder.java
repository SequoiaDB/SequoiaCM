package com.sequoiacm.sequoiadb.dataservice;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.sequoiadb.base.Sequoiadb;

class SequoiadbRecordKey {
    private int siteId;
    private String sdbAddress;

    public SequoiadbRecordKey(int siteId, String sdbAddr) {
        this.siteId = siteId;
        this.sdbAddress = sdbAddr;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public String getSdbAddress() {
        return sdbAddress;
    }

    public void setSdbAddress(String sdbAddress) {
        this.sdbAddress = sdbAddress;
    }

    @Override
    public int hashCode() {
        int hashCode = sdbAddress.hashCode() + siteId;
        return hashCode;
    }

    @Override
    public boolean equals(Object r) {
        if (r instanceof SequoiadbRecordKey) {
            SequoiadbRecordKey right = (SequoiadbRecordKey)r;
            return siteId == right.siteId && sdbAddress.equals(right.sdbAddress);
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("siteId:").append(siteId).append(',');
        sb.append("address:").append(sdbAddress);

        return sb.toString();
    }
}

public class SequoiadbRecorder {
    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private Map<SequoiadbRecordKey, String> mapSdb = new HashMap<>();

    private boolean isNeedRecording = false;

    private static SequoiadbRecorder recorder = new SequoiadbRecorder();

    public static SequoiadbRecorder getInstance() {
        return recorder;
    }

    private String getStackTrace(StackTraceElement[] traces) {
        StringBuilder sb = new StringBuilder();
        //in order to record less info, we just cut unnecessary level of trace
        //start from 2 level upper, stop at 2 level lower

        int start = 2;
        int length = traces.length - 2 - 2;
        for ( int i = start; i < length; i++) {
            sb.append(traces[i]).append('\n');
        }

        return sb.toString();
    }

    public void startRecord() {
        WriteLock wl = rwLock.writeLock();
        wl.lock();
        try {
            mapSdb.clear();
        }
        finally {
            wl.unlock();
        }

        isNeedRecording = true;
    }

    public void stopRecord() {
        isNeedRecording = false;

        WriteLock wl = rwLock.writeLock();
        wl.lock();
        try {
            mapSdb.clear();
        }
        finally {
            wl.unlock();
        }
    }

    public void record(int siteId, Sequoiadb sdb) {
        if (!isNeedRecording) {
            return;
        }

        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        WriteLock wl = rwLock.writeLock();
        wl.lock();
        try {
            mapSdb.put(new SequoiadbRecordKey(siteId, sdb.toString()),
                    sdb.getServerAddress().toString() + '\n' + getStackTrace(traces));
        }
        finally {
            wl.unlock();
        }
    }

    public void unrecord(int siteId, Sequoiadb sdb) {
        if (!isNeedRecording) {
            return;
        }

        WriteLock wl = rwLock.writeLock();
        wl.lock();
        try {
            mapSdb.remove(new SequoiadbRecordKey(siteId, sdb.toString()));
        }
        finally {
            wl.unlock();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        ReadLock rl = rwLock.readLock();
        rl.lock();
        try {
            Set<Map.Entry<SequoiadbRecordKey, String>> entrySetter = mapSdb.entrySet();
            for (Map.Entry<SequoiadbRecordKey, String> entry : entrySetter) {
                sb.append(entry.getKey().toString()).append('\n');
                sb.append(entry.getValue()).append('\n');
            }
        }
        finally {
            rl.unlock();
        }

        if (sb.length() > 0) {
            return sb.substring(0, sb.length()-1);
        }

        return sb.toString();
    }
}


