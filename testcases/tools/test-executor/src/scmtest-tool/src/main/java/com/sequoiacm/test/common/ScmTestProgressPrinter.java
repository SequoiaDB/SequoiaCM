package com.sequoiacm.test.common;

import com.sequoiacm.test.exec.ScmTestProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ScmTestProgressPrinter {

    private final static Logger logger = LoggerFactory.getLogger(ScmTestProgressPrinter.class);
    private List<ScmTestProgress> testProgresses;
    private String currentTestStep;

    public ScmTestProgressPrinter(List<ScmTestProgress> testProgresses, String currentTestStep) {
        this.testProgresses = testProgresses;
        this.currentTestStep = currentTestStep;
    }

    public void printUntilAllTestCompleted() throws Exception {
        while (true) {
            printTestProgress();
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
        logger.info(currentTestStep + " : total:" + total + ", success:" + success + ", failed:" + failed
                + ", skipped:" + skipped + System.lineSeparator() + progressOfEachHost
                + System.lineSeparator());
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
