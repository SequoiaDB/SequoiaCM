package com.sequoiacm.perf.operation;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.perf.common.ApiMethod;
import com.sequoiacm.perf.common.ApiType;
import com.sequoiacm.perf.config.Config;
import com.sequoiacm.perf.config.ConfigException;
import com.sequoiacm.perf.driver.Driver;
import com.sequoiacm.perf.driver.DriverDownloadThread;
import com.sequoiacm.perf.driver.DriverUploadThread;
import com.sequoiacm.perf.tool.PerfTimer;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class DriverOperation extends BaseOperation {

    private Config config;

    public DriverOperation(Config config) {
        this.config = config;
    }


    private void upload() throws InterruptedException {
        Driver driver = new Driver(config.getDriverUrl(), config.getUser(), config.getPassword());

        long total = 0;
        List<Long> threadTimeList = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(config.getThreadNum());
        PerfTimer perfTimer = new PerfTimer();
        perfTimer.start();

        for (int i = 0; i < config.getThreadNum(); i++) {

            DriverUploadThread uploadThread = null;

            if (config.isReadFromDisk()) {
                String fileReadPath = config.getFileReadPath();
                if (StringUtils.isEmpty(fileReadPath)) {
                    throw new ConfigException("the fileReadPath must be given when readFromDisk was set true");
                }

                uploadThread = new DriverUploadThread(driver, config, fileReadPath, latch);
            } else {
                Random rand = new Random();
                String fileName = "file" + rand.nextInt();
                uploadThread = new DriverUploadThread(driver, config, fileName, latch);
            }

            FutureTask<Long> task = new FutureTask<>(uploadThread);
            Thread thread = new Thread(task);
            thread.setName("driverUpThread-" + i);
            thread.start();

            try {
                threadTimeList.add(task.get());
                total += task.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        latch.await();
        perfTimer.stop();

        long duration = perfTimer.duration();

        recordToExcel("driverUpload");
        recordToText();

        long average = 0;

        for (Long time : threadTimeList) {
            average += (time / config.getFileNum());
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

        long total = 0;
        List<Long> threadTimeList = new ArrayList<>();


        String downloadPath = getDownloadPath(config);

        CountDownLatch latch = new CountDownLatch(config.getThreadNum());
        PerfTimer perfTimer = new PerfTimer();
        perfTimer.start();

        for (int i = 0; i < config.getThreadNum(); i++) {
            try {
                DriverDownloadThread driverDownloadThread = new DriverDownloadThread(config, driver, fileId, downloadPath, latch);
                FutureTask<Long> task = new FutureTask<>(driverDownloadThread);
                Thread thread = new Thread(task);
                thread.setName("driverDownThread-" + i);
                thread.start();
                threadTimeList.add(task.get());
                total += task.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        latch.await();
        perfTimer.stop();

        long duration = perfTimer.duration();

        recordToExcel("driverDownload");
        recordToText();

        long average = 0;

        for (Long time : threadTimeList) {
            average += (time / config.getFileNum());
        }

        average=average/config.getThreadNum();


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
