package com.sequoiacm.diagnose.printer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public abstract class ProgressPrinter extends Thread {
    private Logger logger = LoggerFactory.getLogger(ProgressPrinter.class);
    protected static final long PRINT_PERIOD = 1000;
    protected double avg;
    protected int lastPrintLen;
    private boolean loop = true;

    public void stopPrint() {
        this.loop = false;
    }

    @Override
    public void run() {
        try {
            while (true) {
                printProgress();
                if (!loop) {
                    printProgress();
                    break;
                }
                TimeUnit.MILLISECONDS.sleep(PRINT_PERIOD);
            }
        }
        catch (Exception e) {
            logger.error("Failed to print progress", e);
        }
    }

    private void printProgress() {
        // 输出退格，清空上一次动态打屏的结果
        for (int i = 0; i < lastPrintLen; i++) {
            System.out.print("\b");
        }

        String currentProgress = generateProgress();
        System.out.print(currentProgress);
        lastPrintLen = currentProgress.length();
    }

    public abstract String generateProgress();
}
