package com.sequoiacm.diagnose.printer;

import com.sequoiacm.diagnose.common.CompareInfo;
import com.sequoiacm.diagnose.config.WorkPathConfig;
import com.sequoiacm.diagnose.utils.CommonUtils;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ComparePrinter extends ProgressPrinter {
    private CompareInfo compareInfo;
    private long startTime;

    public ComparePrinter(CompareInfo info) {
        this.compareInfo = info;
        this.startTime = System.currentTimeMillis();
        initPrint();
    }

    public void initPrint() {
        StringBuilder progress = new StringBuilder().append("[").append(CommonUtils.getSystemTime())
                .append("] ").append("workspace: ").append(compareInfo.getWorkspace()).append(", ")
                .append("beginTime: ").append(compareInfo.getBeginTime()).append(", ")
                .append("endTime: ").append(compareInfo.getEndTime()).append(", ").append("level: ")
                .append(compareInfo.getCheckLevel()).append(", ").append("file_total: ")
                .append(compareInfo.getProgress().getTotalCount());
        System.out.println(progress);
        lastPrintLen = progress.toString().length();
    }

    @Override
    public String generateProgress() {
        StringBuilder progress = new StringBuilder().append("[").append(CommonUtils.getSystemTime())
                .append("] ").append("workspace: ").append(compareInfo.getWorkspace()).append(", ")
                .append(compareInfo.getProgress().toString()).append(", ")
                .append("will take about: ").append(getEstimatedRuntime());
        return progress.toString();
    }

    private String getEstimatedRuntime() {
        double progressCount = compareInfo.getProgress().getProgressCount().get();
        double totalCount = compareInfo.getProgress().getTotalCount();
        if (progressCount > 0) {
            // 运行时长 / 已比对的文件数
            this.avg = (System.currentTimeMillis() - startTime) / progressCount;
        }
        double willCheckCount = totalCount - progressCount;
        double estimatedRuntimeMs = avg * willCheckCount;
        return CommonUtils.covertMillisToMinAndSeconds(estimatedRuntimeMs);
    }

    public void finish() throws ScmToolsException {
        stopPrint();
        System.out.println();
        StringBuilder progress = new StringBuilder().append("[").append(CommonUtils.getSystemTime())
                .append("] ").append("Finish! ").append("workspace: ")
                .append(compareInfo.getWorkspace()).append(", ")
                .append(compareInfo.getProgress().toString()).append(", ").append("cost: ")
                .append(CommonUtils
                        .covertMillisToMinAndSeconds(System.currentTimeMillis() - startTime))
                .append(", ").append("the result in: ")
                .append(WorkPathConfig.getInstance().getCompareResultPath());
        System.out.println(progress);
    }
}
