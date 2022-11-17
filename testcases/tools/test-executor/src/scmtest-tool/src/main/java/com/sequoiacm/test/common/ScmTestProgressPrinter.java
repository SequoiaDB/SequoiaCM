package com.sequoiacm.test.common;

import com.sequoiacm.test.exec.ScmTestProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
            printTestProgress();
            checkFailedProgress();
            if (isProgressComplete(testProgresses)) {
                printTestProgress();
                break;
            }
        }
    }

    private void printTestProgress() throws Exception {
        StringBuilder progressOfEachHost = new StringBuilder();
        int total = 0, success = 0, failed = 0, skipped = 0;

        long needWaitedTime = 5000;
        long startTime = System.currentTimeMillis();
        for (ScmTestProgress testProgress : testProgresses) {
            long waitedTime = System.currentTimeMillis() - startTime;
            if (waitedTime < needWaitedTime) {
                testProgress.waitTestComplete(needWaitedTime - waitedTime);
            }

            testProgress.updateProgress();
            if (testProgress.getTotal() != -1) {
                total += testProgress.getTotal();
            }
            success += testProgress.getSuccessCount();
            failed += testProgress.getFailedCount();
            skipped += testProgress.getSkippedCount();
            progressOfEachHost.append(testProgress).append(System.lineSeparator());
        }

        if (ShutdownHookMgr.getInstance().isShutDown()) {
            throw new InterruptedException("The process was interrupted");
        }
        String currentTime = new SimpleDateFormat(" [HH:mm:ss]").format(new Date());
        logger.info(currentTestStep + " : total:" + total + ", success:" + success + ", failed:"
                + failed + ", skipped:" + skipped + currentTime + System.lineSeparator()
                + progressOfEachHost + System.lineSeparator());
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
