package com.sequoiacm.s3import.module;

import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.s3import.exception.S3ImportExitCode;

public class CompareResult {

    public final static String RECORD_SEPARATOR = ",";

    private String diffType;
    private String key;

    public CompareResult(String recordStr) throws ScmToolsException {
        String[] split = recordStr.split(RECORD_SEPARATOR);
        if (split.length != 2) {
            throw new ScmToolsException(
                    "Failed to parse compare record of comparison result, record:" + recordStr,
                    S3ImportExitCode.INVALID_ARG);
        }
        this.diffType = split[0];
        this.key = split[1];
    }

    public CompareResult(String key, String diffType) {
        this.key = key;
        this.diffType = diffType;
    }

    public String getDiffType() {
        return diffType;
    }

    public void setDiffType(String diffType) {
        this.diffType = diffType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return diffType + RECORD_SEPARATOR + key;
    }
}
