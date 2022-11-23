package com.sequoiacm.datasource.dataoperation;

import java.util.Objects;

public class ScmSpacePartitionInfo {

    private int recyclingCount;

    private String csName;

    public ScmSpacePartitionInfo(String csName) {
        this.csName = csName;
    }

    public int getRecyclingCount() {
        return recyclingCount;
    }

    public void setRecyclingCount(int recyclingCount) {
        this.recyclingCount = recyclingCount;
    }

    public String getCsName() {
        return csName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ScmSpacePartitionInfo that = (ScmSpacePartitionInfo) o;

        return Objects.equals(csName, that.csName);
    }

    @Override
    public int hashCode() {
        return csName != null ? csName.hashCode() : 0;
    }
}
