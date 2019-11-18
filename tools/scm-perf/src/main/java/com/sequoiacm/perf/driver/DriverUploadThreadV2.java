package com.sequoiacm.perf.driver;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.perf.config.Config;
import com.sequoiacm.perf.config.ConfigException;
import com.sequoiacm.perf.tool.PerfTimer;
import com.sequoiacm.perf.tool.Recorder;
import com.sequoiacm.perf.vo.RecordVo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class DriverUploadThreadV2 implements Runnable {

    private Recorder recorder = Recorder.getInstance();

    private CountDownLatch latch;

    private Config config;
    private Driver driver;
    private String fileName;
    private AtomicInteger counter;

    public DriverUploadThreadV2(Driver driver, Config config, String fileName,
                                CountDownLatch latch, AtomicInteger counter) {
        this.driver = driver;
        this.config = config;
        this.fileName = fileName;
        this.latch = latch;
        this.counter = counter;
    }

    @Override
    public void run() {

        PerfTimer perfTimer = new PerfTimer();

        perfTimer.start();

        int count = 0;

        while (counter.decrementAndGet() >= 0) {
            if (config.isReadFromDisk()) {
                File file = new File(fileName);
                if (!file.isFile()) {
                    throw new ConfigException("fileReadPath is an invalid file path");
                }
                try {
                    FileInputStream fis = new FileInputStream(fileName);
                    driver.upload(config.getWorkspace(), fileName, fis);
                    fis.close();
                } catch (ScmException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Random rand = new Random();
                byte[] fileBytes = new byte[config.getFileSize() * 1024];
                rand.nextBytes(fileBytes);
                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
                    driver.upload(config.getWorkspace(), fileName, bais);
                    bais.close();
                } catch (ScmException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

        if (config.isReadFromDisk()) {
            File file = new File(config.getFileReadPath());
            recordVo.setFileSize(file.length() / 1024);
        } else {
            recordVo.setFileSize(config.getFileSize());
        }
        recorder.record(Thread.currentThread().getName(), recordVo);
    }
}
