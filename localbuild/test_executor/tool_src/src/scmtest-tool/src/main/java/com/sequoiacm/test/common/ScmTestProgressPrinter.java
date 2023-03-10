package com.sequoiacm.test.common;

import com.sequoiacm.test.exec.ScmTestProgress;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.sequoiacm.test.config.LocalPathConfig.TEST_RESULT_PATH;

public class ScmTestProgressPrinter {

    private final static Logger logger = LoggerFactory.getLogger(ScmTestProgressPrinter.class);
    private ScmTestRunner.ReportCollectCallback reportCollectCallback;
    private List<ScmTestProgress> testProgresses;
    private String currentTestStep;

    public ScmTestProgressPrinter(List<ScmTestProgress> testProgresses, String currentTestStep,
            ScmTestRunner.ReportCollectCallback reportCollectCallback) {
        this.testProgresses = testProgresses;
        this.currentTestStep = currentTestStep;
        this.reportCollectCallback = reportCollectCallback;
    }

    public void printUntilAllTestCompleted() throws Exception {
        while (true) {
            ProgressDetail progressDetail = generateProgressDetail();
            logger.info(progressDetail.toString());
            checkFailedProgress();

            if (isProgressComplete(testProgresses)) {
                ProgressDetail finalProgressDetail = generateProgressDetail();
                // 持久化本次用例执行结果，仅保留汇总结果，各个主机的执行进度不做记录
                FileUtils.writeStringToFile(new File(TEST_RESULT_PATH),
                        finalProgressDetail.getProgressSummary(), Charsets.UTF_8, true);
                logger.info(finalProgressDetail.toString());
                break;
            }
        }
    }

    private ProgressDetail generateProgressDetail() throws Exception {
        ProgressDetail progressDetail = new ProgressDetail(currentTestStep);

        long needWaitedTime = 5000;
        long startTime = System.currentTimeMillis();
        for (ScmTestProgress testProgress : testProgresses) {
            long waitedTime = System.currentTimeMillis() - startTime;
            if (waitedTime < needWaitedTime) {
                testProgress.waitTestComplete(needWaitedTime - waitedTime);
            }
            // 从执行机上回收用例执行进度
            testProgress.updateProgress();
        }
        progressDetail.setProgressList(testProgresses);
        
        if (ShutdownHookMgr.getInstance().isShutDown()) {
            throw new InterruptedException("The process was interrupted");
        }
        return progressDetail;
    }

    private void checkFailedProgress() {
        List<ScmTestProgress> failedProgresses = new ArrayList<>();
        for (ScmTestProgress testProgress : testProgresses) {
            if (testProgress.isFailedCountChanged()) {
                failedProgresses.add(testProgress);
            }
        }
        if (failedProgresses.size() != 0) {
            reportCollectCallback.callback(failedProgresses);
        }
    }

    private boolean isProgressComplete(List<ScmTestProgress> scmTestProgresses) {
        for (ScmTestProgress scmTestProgress : scmTestProgresses) {
            if (!scmTestProgress.isComplete()) {
                return false;
            }
        }
        return true;
    }
}

class ProgressDetail {

    private String currentTestStep;
    private int total = 0;
    private int success = 0;
    private int failed = 0;
    private int skipped = 0;
    private List<ScmTestProgress> progressList = new ArrayList<>();

    public ProgressDetail(String currentTestStep) {
        this.currentTestStep = currentTestStep;
    }

    // e.g.
    // (1/1) testng : total:687, success:471, failed:31, skipped:157 [10:50:02]
    public String getProgressSummary() {
        StringBuilder progressSummary = new StringBuilder();
        String currentTime = new SimpleDateFormat(" [HH:mm:ss]").format(new Date());
        progressSummary.append(currentTestStep).append(" : total:").append(total)
                .append(", success:").append(success).append(", failed:").append(failed)
                .append(", skipped:").append(skipped).append(currentTime)
                .append(System.lineSeparator());
        return progressSummary.toString();
    }

    public void setProgressList(List<ScmTestProgress> progressList) {
        this.progressList = progressList;
        for (ScmTestProgress progress : progressList) {
            if (progress.getTotal() != -1) {
                total += progress.getTotal();
            }
            success += progress.getSuccessCount();
            failed += progress.getFailedCount();
            skipped += progress.getSkippedCount();
        }
    }

    // e.g.
    // (1/1) testng : total:690, success:496, failed:33, skipped:160 [10:55:25]
    // 192.168.16.66 | total:490, success:496, failed:33, skipped:160
    // 192.168.31.11 | total:200, success:200, failed:0, skipped:0
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append(getProgressSummary());
        for (int i = 0; i < progressList.size(); i++) {
            if (i != 0) {
                sb.append(System.lineSeparator());
            }
            sb.append(progressList.get(i));
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
