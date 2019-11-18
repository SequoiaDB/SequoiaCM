package com.sequoiacm.perf.rest;

import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.perf.config.Config;
import com.sequoiacm.perf.tool.PerfTimer;
import com.sequoiacm.perf.tool.Recorder;
import com.sequoiacm.perf.vo.RecordVo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class RestDownloadThreadV2 implements Runnable {


    Recorder recorder = Recorder.getInstance();

    private AtomicInteger counter;
    private CountDownLatch latch;

    private Rest rest;
    private String fileId;
    private Config config;
    private String filePath;


    public RestDownloadThreadV2(Config config, Rest rest, String fileId, String filePath,
                                AtomicInteger counter, CountDownLatch latch) {
        this.counter = counter;
        this.latch = latch;
        this.rest = rest;
        this.fileId = fileId;
        this.filePath = filePath;
        this.config = config;
    }

    @Override
    public void run() {

        PerfTimer perfTimer = new PerfTimer();

        perfTimer.start();

        int count = 0;

        String pathName = filePath + File.separator + Thread.currentThread().getName() + System.currentTimeMillis() / 1000;
        while (counter.decrementAndGet() >= 0) {
            try {
                OutputStream fos =
                        new FileOutputStream(
                                new File(pathName + count));
                rest.download(config.getWorkspace(), fileId, fos);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (ScmException e) {
                e.printStackTrace();
            }
            latch.countDown();
            count++;
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
        recordVo.setFileSize(config.getFileSize());

        recorder.record(Thread.currentThread().getName(), recordVo);
    }
}
