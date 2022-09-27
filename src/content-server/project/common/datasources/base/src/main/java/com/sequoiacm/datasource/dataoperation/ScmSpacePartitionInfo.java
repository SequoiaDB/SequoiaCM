package com.sequoiacm.datasource.dataoperation;

import java.util.Objects;

public class ScmSpacePartitionInfo {

    private String shardingStr;

    private int recyclingCount;

    public ScmSpacePartitionInfo(String shardingStr) {
        this.shardingStr = shardingStr;
    }

    public String getShardingStr() {
        return shardingStr;
    }

    public void setShardingStr(String shardingStr) {
        this.shardingStr = shardingStr;
    }

    public int getRecyclingCount() {
        return recyclingCount;
    }

    public void setRecyclingCount(int recyclingCount) {
        this.recyclingCount = recyclingCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ScmSpacePartitionInfo that = (ScmSpacePartitionInfo) o;

        return Objects.equals(shardingStr, that.shardingStr);
    }

    @Override
    public int hashCode() {
        return shardingStr != null ? shardingStr.hashCode() : 0;
    }
}
