package com.sequoiacm.datasource.dataoperation;

public enum ENDataType {
    Normal(1);

    private int value;
    private ENDataType(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
