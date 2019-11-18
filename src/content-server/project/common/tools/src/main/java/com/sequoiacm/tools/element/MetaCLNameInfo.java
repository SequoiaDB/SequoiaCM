package com.sequoiacm.tools.element;

public class MetaCLNameInfo {
    private String clName = "";
    private String clHistoryName = "";

    private String lowMonth = "";
    private String upperMonth = "";

    public MetaCLNameInfo(String clName, String clHistoryName, String lowMonth, String upperMonth) {
        this.clName = clName;
        this.clHistoryName = clHistoryName;
        this.lowMonth = lowMonth;
        this.upperMonth = upperMonth;
    }

    public String getClName() {
        return clName;
    }

    public String getClHistoryName() {
        return clHistoryName;
    }

    public String getLowMonth() {
        return lowMonth;
    }

    public String getUpperMonth() {
        return upperMonth;
    }
}
