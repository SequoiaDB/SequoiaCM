package com.sequoiacm.perf.operation;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.perf.config.Config;
import com.sequoiacm.perf.config.ConfigException;
import com.sequoiacm.perf.driver.Driver;
import com.sequoiacm.perf.driver.DriverDownloadThreadV2;
import com.sequoiacm.perf.driver.DriverUploadThreadV2;
import com.sequoiacm.perf.tool.PerfTimer;
import com.sequoiacm.perf.tool.Recorder;
import com.sequoiacm.perf.vo.RecordVo;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class DriverOperationV2 extends BaseOperation {

    private Config config;

    public DriverOperationV2(Config config) {
        this.config = config;
    }


    private void upload() throws InterruptedException {
        Driver driver = new Driver(config.getDriverUrl(), config.getUser(), config.getPassword());
        CountDownLatch latch = new CountDownLatch(config.getFileNum());
        AtomicInteger counter = new AtomicInteger(config.getFileNum());
        PerfTimer perfTimer = new PerfTimer();
        perfTimer.start();

        for (int i = 0; i < config.getThreadNum(); i++) {

            DriverUploadThreadV2 uploadThread = null;

            if (config.isReadFromDisk()) {
                String fileReadPath = config.getFileReadPath();
                if (StringUtils.isEmpty(fileReadPath)) {
                    throw new ConfigException("the fileReadPath must be given when readFromDisk was set true");
                }
                uploadThread = new DriverUploadThreadV2(driver, config, fileReadPath, latch, counter);
            } else {
                Random rand = new Random();
                String fileName = "file" + rand.nextInt();
                uploadThread = new DriverUploadThreadV2(driver, config, fileName, latch, counter);
            }

            Thread thread = new Thread(uploadThread);
            thread.setName("driverUpThread-" + i);
            thread.start();
        }

        latch.await();
        perfTimer.stop();

        long duration = perfTimer.duration();

        recordToText();

        Long average = 0L;
        Map<String, RecordVo> recordMap = Recorder.getInstance().getRecordMap();

        for (String s : recordMap.keySet()) {
            average+=recordMap.get(s).getAverage();
        }
        average = average / config.getThreadNum();

        long fileSize = 0;

        if (config.isReadFromDisk()) {
            File file = new File(config.getFileReadPath());
            fileSize = file.length() / 1024;
        } else {
            fileSize = config.getFileSize();
        }

        printSummarize(config.getThreadNum(), duration, config.getFileNum(), fileSize, average);
    }


    private void download() throws InterruptedException, ScmException {
        Driver driver = new Driver(config.getDriverUrl(), config.getUser(), config.getPassword());
        Random rand = new Random();
        byte[] fileBytes = new byte[config.getFileSize() * 1024];
        rand.nextBytes(fileBytes);
        String fileId = driver.upload(config.getWorkspace(), String.valueOf(new Random().nextInt()), new ByteArrayInputStream(fileBytes));


        String downloadPath = getDownloadPath(config);
        CountDownLatch latch = new CountDownLatch(config.getFileNum());
        AtomicInteger counter = new AtomicInteger(config.getFileNum());


        PerfTimer perfTimer = new PerfTimer();
        perfTimer.start();

        for (int i = 0; i < config.getThreadNum(); i++) {
                DriverDownloadThreadV2 driverDownloadThread =
                        new DriverDownloadThreadV2(config, driver, fileId, downloadPath, latch, counter);
                Thread thread = new Thread(driverDownloadThread);
                thread.setName("driverDownThread-" + i);
                thread.start();

        }

        latch.await();
        perfTimer.stop();

        long duration = perfTimer.duration();

        recordToText();

        long average = 0;
        Map<String, RecordVo> recordMap = Recorder.getInstance().getRecordMap();

        for (String s : recordMap.keySet()) {
            average+=recordMap.get(s).getAverage();
        }

        average = average / config.getThreadNum();
        printSummarize(config.getThreadNum(), duration, config.getFileNum(), config.getFileSize(), average);
    }


    public void run() throws InterruptedException, ScmException {
        switch (config.getApiMethod()) {
            case UPLOAD:
                upload();
                break;
            case DOWNLOAD:
                download();
                break;
            default:
                break;
        }
    }
}
