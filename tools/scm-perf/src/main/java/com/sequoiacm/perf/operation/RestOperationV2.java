package com.sequoiacm.perf.operation;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.perf.config.Config;
import com.sequoiacm.perf.config.ConfigException;
import com.sequoiacm.perf.rest.Rest;
import com.sequoiacm.perf.rest.RestDownloadThreadV2;
import com.sequoiacm.perf.rest.RestUploadThreadV2;
import com.sequoiacm.perf.tool.PerfTimer;
import com.sequoiacm.perf.tool.Recorder;
import com.sequoiacm.perf.vo.RecordVo;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RestOperationV2 extends BaseOperation {

    private Config config;

    public RestOperationV2(Config config) {
        this.config = config;
    }


    private void upload() throws InterruptedException {
        Rest rest = new Rest(config.getRestUrl(), config.getUser(), config.getPassword());
        CountDownLatch latch = new CountDownLatch(config.getFileNum());
        AtomicInteger counter = new AtomicInteger(config.getFileNum());


        PerfTimer perfTimer = new PerfTimer();
        perfTimer.start();
        for (int i = 0; i < config.getThreadNum(); i++) {

            RestUploadThreadV2 uploadThread = null;

            if (config.isReadFromDisk()) {
                String fileReadPath = config.getFileReadPath();
                if (StringUtils.isEmpty(fileReadPath)) {
                    throw new ConfigException("the fileReadPath must be given when readFromDisk was set true");
                }
                uploadThread = new RestUploadThreadV2(rest, config, fileReadPath, null, latch,counter);

            } else {
                Random rand = new Random();
                byte[] fileBytes = new byte[config.getFileSize() * 1024];
                rand.nextBytes(fileBytes);
                String fileName = "file" + rand.nextInt();
                uploadThread = new RestUploadThreadV2(rest, config, fileName, fileBytes, latch,counter);
            }

            Thread thread = new Thread(uploadThread);
            thread.setName("restUpThread-" + i);
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

        String downloadPath = getDownloadPath(config);

        CountDownLatch latch = new CountDownLatch(config.getFileNum());
        AtomicInteger counter = new AtomicInteger(config.getFileNum());
        PerfTimer perfTimer = new PerfTimer();
        perfTimer.start();

        for (int i = 0; i < config.getThreadNum(); i++) {
            RestDownloadThreadV2 restDownloadThread = new RestDownloadThreadV2(config, rest, fileId, downloadPath, counter,latch);
            Thread thread = new Thread(restDownloadThread);
            thread.setName("restDownThread-" + i);
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
