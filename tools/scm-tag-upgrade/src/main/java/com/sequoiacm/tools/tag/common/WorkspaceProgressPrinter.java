package com.sequoiacm.tools.tag.common;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class WorkspaceProgressPrinter implements FileFinishCallback {
    private final int currentWsSerialNum;
    private final int totalWsCount;
    private final String ws;
    private final AtomicInteger failedFiles;
    private final long totalFile;
    private final AtomicLong processedFile;

    private long lastPrintTime;
    private long lastPrintProcessedFile;
    private int backspaceCount = 0;

    public WorkspaceProgressPrinter(UpgradeTagStatus upgradeStatus) {
        this(upgradeStatus.getCurrentWorkspace(),
                upgradeStatus.getWsList().indexOf(upgradeStatus.getCurrentWorkspace()) + 1,
                upgradeStatus.getWsList().size(), upgradeStatus.getCurrentWorkspaceFileCount(),
                upgradeStatus.getCurrentWorkspaceProcessedFileCount(),
                upgradeStatus.getWorkspaceFailedFileCount(upgradeStatus.getCurrentWorkspace()));
    }

    private WorkspaceProgressPrinter(String ws, int currentWsSerialNum, int totalWsCount,
            long totalFile, long processedFile, int failedFiles) {
        this.ws = ws;
        this.totalFile = totalFile;
        this.processedFile = new AtomicLong(processedFile);
        this.failedFiles = new AtomicInteger(failedFiles);
        this.currentWsSerialNum = currentWsSerialNum;
        this.totalWsCount = totalWsCount;
    }

    public void printBegin() {
        System.out.printf("Processing workspace(%d/%d): %s%n", currentWsSerialNum, totalWsCount,
                ws);
        printUserTagProgress(processedFile.get(), failedFiles.get());
    }

    public void incProcessedFile(boolean isSuccess) {
        long newProcessedFile = processedFile.incrementAndGet();
        int newFailedFile = 0;
        if (!isSuccess) {
            newFailedFile = failedFiles.incrementAndGet();
        }
        else {
            newFailedFile = failedFiles.get();
        }
        if (newProcessedFile - lastPrintProcessedFile > 300
                || System.currentTimeMillis() - lastPrintTime > 10 * 1000) {
            synchronized (this) {
                printUserTagProgress(newProcessedFile, newFailedFile);
            }
        }

    }

    public void printEmptyCustomTagProcessing() {
        System.out.println("Refactoring empty custom tag...");
    }

    private void printUserTagProgress(long alreadyProcessedFile, int failedFileCount) {
        int progress = (int) (((double) alreadyProcessedFile) / totalFile * 100);
        progress = progress > 100 ? 99 : progress;
        lastPrintTime = System.currentTimeMillis();
        lastPrintProcessedFile = alreadyProcessedFile;
        printBackspace(backspaceCount);
        String msg = String.format(
                "Refactoring user tag, TotalFiles: %d, ProcessedFiles %d, FailedFiles: %d, Progress: %d%%",
                totalFile, lastPrintProcessedFile, failedFileCount, progress);
        System.out.print(msg);
        backspaceCount = msg.length();
    }

    public void printUserTagProgressEnd(long processedFile, int failedFile) {
        printBackspace(backspaceCount);
        System.out.printf(
                "Refactoring user tag, TotalFiles: %d, ProcessedFiles %d, FailedFiles: %d, Progress: %d%%%n",
                processedFile, processedFile, failedFile, 100);
    }

    private void printBackspace(int count) {
        for (int i = 0; i < count; i++) {
            System.out.print("\b");
        }
    }

    @Override
    public void onFileFinish(String ws, FileBasicInfo fileInfo, boolean success) {
        incProcessedFile(success);
    }
}
