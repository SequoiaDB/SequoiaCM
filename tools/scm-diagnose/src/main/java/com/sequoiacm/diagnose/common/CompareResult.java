package com.sequoiacm.diagnose.common;

public class CompareResult {
    private String fileId;
    private String siteName;
    private int majorVersion;
    private int minorVersion;
    private ResultType resultType;
    private String detail;

    public CompareResult(String fileId, String siteName, int majorVersion, int minorVersion,
            ResultType resultType, String detail) {
        this.fileId = fileId;
        this.siteName = siteName;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.resultType = resultType;
        this.detail = detail;
    }

    public ResultType getResultType() {
        return resultType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append(fileId).append(",").append(siteName)
                .append(",").append(majorVersion).append(",").append(minorVersion).append(",")
                .append(resultType).append(",").append("\"").append(detail).append("\"");
        return builder.toString();
    }
}
