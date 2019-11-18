package com.sequoiacm.perf.driver;

import com.sequoiacm.perf.config.Config;
import com.sequoiacm.perf.config.ConfigException;
import com.sequoiacm.perf.tool.PerfTimer;
import com.sequoiacm.perf.tool.Recorder;
import com.sequoiacm.perf.vo.RecordVo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class DriverUploadThread implements Callable<Long> {

    private Recorder recorder = Recorder.getInstance();

    private CountDownLatch latch;

    private Config config;
    private Driver driver;
    private String fileName;

    public DriverUploadThread(Driver driver, Config config, String fileName,
                              CountDownLatch latch) {
        this.driver = driver;
        this.fileName = fileName;
        this.config = config;
        this.latch = latch;
    }

    @Override
    public Long call() throws Exception {

        PerfTimer perfTimer = new PerfTimer();

        perfTimer.start();

        for (int i = 0; i < config.getFileNum(); i++) {
            if (config.isReadFromDisk()) {
                File file = new File(fileName);
                if (!file.isFile()) {
                    throw new ConfigException("fileReadPath is an invalid file path");
                }
                FileInputStream fis = new FileInputStream(fileName);
                driver.upload(config.getWorkspace(), fileName, fis);
                fis.close();
            } else {
                Random rand = new Random();
                byte[] fileBytes = new byte[config.getFileSize() * 1024];
                rand.nextBytes(fileBytes);
                ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
                driver.upload(config.getWorkspace(), fileName, bais);
                bais.close();
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
