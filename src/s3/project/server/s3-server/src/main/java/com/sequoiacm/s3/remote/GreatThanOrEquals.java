package com.sequoiacm.s3.remote;

import java.util.List;

public class GreatThanOrEquals {
    private List<String> equalsList;
    private String greaterThan;

    public GreatThanOrEquals(String greaterThan, List<String> equalsList) {
        this.greaterThan = greaterThan;
        this.equalsList = equalsList;
    }

    public List<String> getEqualsList() {
        return equalsList;
    }

    public String getGreaterThan() {
        return greaterThan;
    }

    @Override
    public String toString() {
        return "[equalsList=" + equalsList + ", greaterThan=" + greaterThan + "]";
    }

}
