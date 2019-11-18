package com.sequoiacm.perf.operation;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.perf.common.ApiMethod;
import com.sequoiacm.perf.common.ApiType;
import com.sequoiacm.perf.config.Config;
import com.sequoiacm.perf.config.ConfigException;
import com.sequoiacm.perf.rest.Rest;
import com.sequoiacm.perf.rest.RestDownloadThread;
import com.sequoiacm.perf.rest.RestUploadThread;
import com.sequoiacm.perf.tool.PerfTimer;
import org.fusesource.jansi.Ansi;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.ansi;

public class RestOperation extends BaseOperation {

    private Config config;

    public RestOperation(Config config) {
        this.config = config;
    }


    private void upload() throws InterruptedException {
        Rest rest = new Rest(config.getRestUrl(), config.getUser(), config.getPassword());

        long total = 0;
        List<Long> threadTimeList = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(config.getThreadNum());
        PerfTimer perfTimer = new PerfTimer();
        perfTimer.start();

        for (int i = 0; i < config.getThreadNum(); i++) {

            RestUploadThread uploadThread = null;

            if (config.isReadFromDisk()) {
                String fileReadPath = config.getFileReadPath();
                if (StringUtils.isEmpty(fileReadPath)) {
                    throw new ConfigException("the fileReadPath must be given when readFromDisk was set true");
                }

                uploadThread = new RestUploadThread(rest, config, fileReadPath, null, latch);

            } else {
                Random rand = new Random();
                byte[] fileBytes = new byte[config.getFileSize() * 1024];
                rand.nextBytes(fileBytes);
                String fileName = "file" + rand.nextInt();
                uploadThread = new RestUploadThread(rest, config, fileName, fileBytes, latch);
            }


            FutureTask<Long> task = new FutureTask<>(uploadThread);
            Thread thread = new Thread(task);
            thread.setName("restUpThread-" + i);
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

        recordToExcel("restUpload");
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
        Random rand = new Random();
        byte[] fileBytes = new byte[config.getFileSize() * 1024];
        rand.nextBytes(fileBytes);
        Rest rest = new Rest(config.getRestUrl(), config.getUser(), config.getPassword());
        String fileId = rest.upload(config.getWorkspace(), String.valueOf(new Random().nextInt()), fileBytes);

        long total = 0;
        List<Long> threadTimeList = new ArrayList<>();
        String downloadPath = getDownloadPath(config);

        CountDownLatch latch = new CountDownLatch(config.getThreadNum());
        PerfTimer perfTimer = new PerfTimer();
        perfTimer.start();

        for (int i = 0; i < config.getThreadNum(); i++) {
            try {
                RestDownloadThread restDownloadThread = new RestDownloadThread(config, rest, fileId, downloadPath, latch);
                FutureTask<Long> task = new FutureTask<>(restDownloadThread);
                Thread thread = new Thread(task);
                thread.setName("restDownThread-" + i);
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

        recordToExcel("restDownload");
        recordToText();

        long average = 0;

        for (Long time : threadTimeList) {
            average += (time / config.getFileNum());
        }

        average = average / config.getThreadNum();

        printSummarize(config.getThreadNum(), duration, config.getFileNum(), config.getFileSize(),average);
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
