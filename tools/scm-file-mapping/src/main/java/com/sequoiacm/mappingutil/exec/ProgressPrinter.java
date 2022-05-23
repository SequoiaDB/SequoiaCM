package com.sequoiacm.mappingutil.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ProgressPrinter extends Thread {

    private Logger logger = LoggerFactory.getLogger(ProgressPrinter.class);
    private static final long PRINT_PERIOD = 5000;
    private boolean loop = true;
    private MappingProgress progress;

    public ProgressPrinter(MappingProgress progress) {
        this.progress = progress;
    }

    public void stopPrint() {
        this.loop = false;
    }

    @Override
    public void run() {
        try {
            int lastPrintLen = 0;
            while (true) {
                // 输出退格，清空上一次动态打屏的结果
                for (int i = 0; i < lastPrintLen; i++) {
                    System.out.print("\b");
                }

                if (progress.isFinish()) {
                    System.out.println(progress);
                    logger.info(
                            "Finished file mapping, success_count={}, failure_count={}, process_count={}",
                            progress.getSuccessCount(), progress.getErrorCount(),
                            progress.getProcessCount());
                    break;
                }

                if (!loop) {
                    System.out.println(progress);
                    break;
                }

                System.out.print(progress);
                lastPrintLen = progress.toString().length();
                TimeUnit.MILLISECONDS.sleep(PRINT_PERIOD);
            }
        }
        catch (Exception e) {
            logger.error("Failed to print mapping progress", e);
        }
    }
}
