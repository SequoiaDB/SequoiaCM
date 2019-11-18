package com.sequoiacm.perf.rest;

import com.sequoiacm.perf.config.Config;
import com.sequoiacm.perf.tool.PerfTimer;
import com.sequoiacm.perf.tool.Recorder;
import com.sequoiacm.perf.vo.RecordVo;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class RestUploadThread implements Callable<Long> {
    Recorder recorder = Recorder.getInstance();
    private byte[] content;
    private CountDownLatch latch;

    private Config config;
    private Rest rest;
    private String fileName;

    public RestUploadThread(Rest rest, Config config, String fileName,
                            byte[] content, CountDownLatch latch) {
        this.rest = rest;
        this.fileName = fileName;
        this.config = config;
        this.content = content;
        this.latch = latch;
    }

    @Override
    public Long call() throws Exception {
        PerfTimer perfTimer = new PerfTimer();

        perfTimer.start();

        for (int i = 0; i < config.getFileNum(); i++) {

            if (config.isReadFromDisk()) {
                rest.upload(config.getWorkspace(), fileName, null);
            } else {
                rest.upload(config.getWorkspace(), fileName, content);
            }

        }

        perfTimer.stop();
        latch.countDown();

        RecordVo recordVo = new RecordVo();
        recordVo.setThreadName(Thread.currentThread().getName());
        recordVo.setTotal(perfTimer.duration());
        recordVo.setAverage(perfTimer.duration() / config.getFileNum());
        recordVo.setFileNum(config.getFileNum());

        if (config.isReadFromDisk()) {
            File file = new File(config.getFileReadPath());
            recordVo.setFileSize(file.length()/1024);
        } else {
            recordVo.setFileSize(config.getFileSize());
        }


        recorder.record(Thread.currentThread().getName(), recordVo);

        return perfTimer.duration();
    }
}
