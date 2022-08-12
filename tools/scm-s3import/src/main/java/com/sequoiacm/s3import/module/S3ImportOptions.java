package com.sequoiacm.s3import.module;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.common.CommonDefine;
import com.sequoiacm.s3import.common.CommonUtils;
import com.sequoiacm.s3import.common.convertor.ArgumentConvertor;
import com.sequoiacm.s3import.exception.S3ImportExitCode;
import org.apache.commons.cli.CommandLine;
import org.springframework.util.StringUtils;

import java.util.List;

public class S3ImportOptions {

    private String workPath;
    private String confPath;
    private long maxExecTime = -1;
    private boolean resetCompareProgress;
    private String compareResultPath;
    private List<S3Bucket> bucketList;

    public S3ImportOptions(CommandLine cl) throws ScmToolsException {
        this.workPath = cl.getOptionValue(CommonDefine.Option.WORK_PATH);

        if (cl.hasOption(CommonDefine.Option.CONF)) {
            this.confPath = cl.getOptionValue(CommonDefine.Option.CONF);
        }
        if (cl.hasOption(CommonDefine.Option.MAX_EXEC_TIME)) {
            this.maxExecTime = 1000 *
                    (long) ArgumentConvertor.getParse(CommonDefine.Option.MAX_EXEC_TIME
                            , cl.getOptionValue(CommonDefine.Option.MAX_EXEC_TIME));
        }
    }

    public String getWorkPath() {
        return workPath;
    }

    public String getConfPath() {
        return confPath;
    }

    public long getMaxExecTime() {
        return maxExecTime;
    }

    public String getCompareResultPath() {
        return compareResultPath;
    }

    public void setCompareResultPath(String compareResultPath) {
        this.compareResultPath = CommonUtils.getStandardFilePath(compareResultPath);
    }

    public List<S3Bucket> getBucketList() {
        return bucketList;
    }

    public void setBucketList(List<S3Bucket> bucketList) {
        this.bucketList = bucketList;
    }

    public boolean isResetCompareProgress() {
        return resetCompareProgress;
    }

    public void setResetCompareProgress(boolean resetCompareProgress) {
        this.resetCompareProgress = resetCompareProgress;
    }
}
