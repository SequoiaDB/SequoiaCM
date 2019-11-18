package com.sequoiacm.perf.driver;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.perf.config.Config;
import com.sequoiacm.perf.tool.PerfTimer;
import com.sequoiacm.perf.tool.Recorder;
import com.sequoiacm.perf.vo.RecordVo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class DriverDownloadThreadV2 implements Runnable {
    Recorder recorder = Recorder.getInstance();

    private CountDownLatch latch;
    private AtomicInteger counter;

    private Driver driver;
    private String fileId;
    private Config config;
    private String filePath;


    public DriverDownloadThreadV2(Config config, Driver driver, String fileId, String filePath,
                                  CountDownLatch latch, AtomicInteger counter) {
        this.latch = latch;
        this.driver = driver;
        this.fileId = fileId;
        this.filePath = filePath;
        this.config = config;
        this.counter = counter;
    }

    @Override
    public void run() {
        PerfTimer perfTimer = new PerfTimer();

        perfTimer.start();

        String pathName = filePath + File.separator + Thread.currentThread().getName() + System.currentTimeMillis() / 1000;

        int count = 0;

        while (counter.decrementAndGet() >= 0) {
            try {
                OutputStream fos =
                        new FileOutputStream(
                                new File(pathName + count));
                driver.download(config.getWorkspace(), fileId, fos);
            } catch (FileNotFoundException | ScmException e) {
                e.printStackTrace();
            }
            latch.countDown();
            count++;
        }
        perfTimer.stop();
        RecordVo recordVo = new RecordVo();
        recordVo.setThreadName(Thread.currentThread().getName());
        recordVo.setTotal(perfTimer.duration());
        if (count > 0) {
            recordVo.setAverage(perfTimer.duration() / count);
        } else {
            recordVo.setAverage(0);
        }
        recordVo.setFileNum(count);
        recordVo.setFileSize(config.getFileSize());
        recorder.record(Thread.currentThread().getName(), recordVo);
    }
}
