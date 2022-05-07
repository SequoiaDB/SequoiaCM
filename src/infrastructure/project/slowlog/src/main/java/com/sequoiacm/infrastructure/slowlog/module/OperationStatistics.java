package com.sequoiacm.infrastructure.slowlog.module;

public class OperationStatistics {

    private String name;

    private long spend;

    private int count;

    public OperationStatistics(String name, long spend, int count) {
        this.name = name;
        this.spend = spend;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSpend() {
        return spend;
    }

    public void setSpend(long spend) {
        this.spend = spend;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "OperationStatistics{" + "name='" + name + '\'' + ", spend=" + spend + ", count="
                + count + '}';
    }
}
