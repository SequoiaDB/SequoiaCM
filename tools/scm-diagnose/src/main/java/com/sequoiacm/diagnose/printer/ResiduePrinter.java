package com.sequoiacm.diagnose.printer;

import com.sequoiacm.diagnose.common.ResidueCheckInfo;
import com.sequoiacm.diagnose.config.WorkPathConfig;
import com.sequoiacm.diagnose.utils.CommonUtils;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;

public class ResiduePrinter extends ProgressPrinter {
    private ResidueCheckInfo residueCheckInfo;
    private long startTime;

    public ResiduePrinter(ResidueCheckInfo residueCheckInfo) {
        this.residueCheckInfo = residueCheckInfo;
        this.startTime = System.currentTimeMillis();
        initPrint();
    }

    private void initPrint() {
        StringBuilder progress = new StringBuilder().append("[").append(CommonUtils.getSystemTime())
                .append("] ").append("workspace: ").append(residueCheckInfo.getWorkspace())
                .append(", ").append("site_name: ").append(residueCheckInfo.getSite()).append(", ")
                .append("file_total: ").append(residueCheckInfo.getProgress().getTotalCount());
        System.out.println(progress);
        lastPrintLen = progress.toString().length();
    }

    @Override
    public String generateProgress() {
        StringBuilder progress = new StringBuilder().append("[").append(CommonUtils.getSystemTime())
                .append("] ").append("workspace: ").append(residueCheckInfo.getWorkspace())
                .append(", ").append(residueCheckInfo.getProgress().toString()).append(", ")
                .append("will take about: ").append(getEstimatedRuntime());
        return progress.toString();
    }

    private String getEstimatedRuntime() {
        double progressCount = residueCheckInfo.getProgress().getProgressCount().get();
        double totalCount = residueCheckInfo.getProgress().getTotalCount();
        if (progressCount > 0) {
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
                .append(residueCheckInfo.getWorkspace()).append(", ")
                .append(residueCheckInfo.getProgress().toString()).append(", ").append("cost: ")
                .append(CommonUtils
                        .covertMillisToMinAndSeconds(System.currentTimeMillis() - startTime))
                .append(", the result in:")
                .append(WorkPathConfig.getInstance().getResidueResultPath());
        System.out.println(progress);
    }
}
