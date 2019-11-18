package com.sequoiacm.metasource.sequoiadb.config;

import org.bson.BSONObject;

public class SdbClFileInfo {
    private String clName = "";
    private String clHistoryName = "";

    private String lowMonth = "";
    private String upperMonth = "";

    private BSONObject clOptions;

    public SdbClFileInfo(String clName, String clHistoryName, String lowMonth, String upperMonth, BSONObject clOptions) {
        this.clName = clName;
        this.clHistoryName = clHistoryName;
        this.lowMonth = lowMonth;
        this.upperMonth = upperMonth;
        this.clOptions = clOptions;
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

    public void setLowMonth(String lowMonth) {
        this.lowMonth = lowMonth;
    }

    public void setUpperMonth(String upperMonth) {
        this.upperMonth = upperMonth;
    }

    public BSONObject getClOptions() {
        return clOptions;
    }


}
