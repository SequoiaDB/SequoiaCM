package com.sequoiacm.common;

public class ScmMonthRange {
    private String lowBound;
    private String upBound;

    public ScmMonthRange(String lowBound, String upBound) {
        super();
        this.lowBound = lowBound;
        this.upBound = upBound;
    }

    public String getLowBound() {
        return lowBound;
    }

    public String getUpBound() {
        return upBound;
    }

}
