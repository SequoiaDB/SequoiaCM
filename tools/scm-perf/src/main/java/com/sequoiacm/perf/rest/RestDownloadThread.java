package com.sequoiacm.perf.rest;

import com.sequoiacm.perf.config.Config;
import com.sequoiacm.perf.tool.PerfTimer;
import com.sequoiacm.perf.tool.Recorder;
import com.sequoiacm.perf.vo.RecordVo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class RestDownloadThread implements Callable<Long> {


    Recorder recorder = Recorder.getInstance();

    private CountDownLatch latch;

    private Rest rest;
    private String fileId;
    private Config config;
    private String filePath;


    public RestDownloadThread(Config config, Rest rest, String fileId, String filePath,
                              CountDownLatch latch) {
        this.latch = latch;
        this.rest = rest;
        this.fileId = fileId;
        this.filePath = filePath;
        this.config = config;
    }

    @Override
    public Long call() throws Exception {

        PerfTimer perfTimer = new PerfTimer();

        perfTimer.start();

        String pathName = filePath + File.separator + Thread.currentThread().getName() + System.currentTimeMillis() / 1000;

        for (int i = 0; i < config.getFileNum(); i++) {
            OutputStream fos =
                    new FileOutputStream(
                            new File(pathName + i));
            rest.download(config.getWorkspace(), fileId, fos);
        }

        perfTimer.stop();
        latch.countDown();

        RecordVo recordVo = new RecordVo();
        recordVo.setThreadName(Thread.currentThread().getName());
        recordVo.setTotal(perfTimer.duration());
        recordVo.setAverage(perfTimer.duration() / config.getFileNum());
        recordVo.setFileNum(config.getFileNum());
        recordVo.setFileSize(config.getFileSize());
        recorder.record(Thread.currentThread().getName(), recordVo);
        return perfTimer.duration();
    }
}
