package com.sequoiacm.infrastructure.statistics.client.flush;

import com.sequoiacm.infrastructure.statistics.client.ScmStatisticsRawDataReporter;
import com.sequoiacm.infrastructure.statistics.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BreakpointFileStatisticsRawDataFlush implements StatisticsRawDataFlush {

    private static final Logger logger = LoggerFactory
            .getLogger(BreakpointFileStatisticsRawDataFlush.class);


    @Override
    public boolean isNeedFlush(ScmStatisticsRawData rawData) {
        if (rawData == null || !rawData.isSuccess()
                || !ScmStatisticsType.BREAKPOINT_FILE_UPLOAD.equals(rawData.getType())) {
            return false;
        }
        ScmStatisticsBreakpointFileMeta fileMeta = ((ScmStatisticsBreakpointFileRawData) rawData)
                .getFileMeta();
        return fileMeta != null && fileMeta.isComplete();
    }

    @Override
    public ScmStatisticsRawDataReporter.ScmStatisticsFlushCondition getFlushCondition(
            ScmStatisticsRawData rawData) {
        ScmStatisticsBreakpointFileMeta fileMeta = ((ScmStatisticsBreakpointFileRawData) rawData)
                .getFileMeta();
        return new BreakpointFileFlushCondition(fileMeta);
    }

}

class BreakpointFileFlushCondition
        implements ScmStatisticsRawDataReporter.ScmStatisticsFlushCondition {

    private final ScmStatisticsBreakpointFileMeta responseFileMeta;

    public BreakpointFileFlushCondition(ScmStatisticsBreakpointFileMeta responseFileMeta) {
        this.responseFileMeta = responseFileMeta;

    }

    @Override
    public boolean isMatch(ScmStatisticsRawData statisticsRawData) {
        if (statisticsRawData instanceof ScmStatisticsBreakpointFileRawData) {
            ScmStatisticsBreakpointFileRawData breakpointFileRawData = (ScmStatisticsBreakpointFileRawData) statisticsRawData;
            ScmStatisticsBreakpointFileMeta fileMeta = breakpointFileRawData.getFileMeta();
            return fileMeta.equals(responseFileMeta);
        }
        return false;
    }
}
