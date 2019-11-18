package com.sequoiacm.perf.rest;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.perf.config.Config;
import com.sequoiacm.perf.tool.PerfTimer;
import com.sequoiacm.perf.tool.Recorder;
import com.sequoiacm.perf.vo.RecordVo;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


public class RestUploadThreadV2 implements Runnable {
    Recorder recorder = Recorder.getInstance();
    private byte[] content;
    private CountDownLatch latch;
    private String fileName;
    private Config config;
    private Rest rest;
    private AtomicInteger counter;


    public RestUploadThreadV2(Rest rest, Config config, String fileName,
                              byte[] content, CountDownLatch latch, AtomicInteger counter) {
        this.rest = rest;
        this.config = config;
        this.fileName = fileName;
        this.content = content;
        this.latch = latch;
        this.counter = counter;
    }

    @Override
    public void run() {
        PerfTimer perfTimer = new PerfTimer();

        perfTimer.start();

        int count = 0;

        while (counter.decrementAndGet() >= 0) {
            try {
                if (config.isReadFromDisk()) {
                    rest.upload(config.getWorkspace(), fileName, null);
                } else {
                    rest.upload(config.getWorkspace(), fileName, content);
                }
            } catch (ScmException e) {
                e.printStackTrace();
            }
            count++;
            latch.countDown();
        }

        perfTimer.stop();

        RecordVo recordVo = new RecordVo();
        recordVo.setThreadName(Thread.currentThread().getName());
        recordVo.setTotal(perfTimer.duration());

        if (count != 0) {
            recordVo.setAverage(perfTimer.duration() / count);
        } else {
            recordVo.setAverage(0);
        }
        recordVo.setFileNum(count);

        if (config.isReadFromDisk()) {
            File file = new File(config.getFileReadPath());
            recordVo.setFileSize(file.length() / 1024);
        } else {
            recordVo.setFileSize(config.getFileSize());
        }
        recorder.record(Thread.currentThread().getName(), recordVo);
    }
}
