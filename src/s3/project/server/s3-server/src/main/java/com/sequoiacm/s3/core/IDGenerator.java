package com.sequoiacm.s3.core;

public class IDGenerator {
    public static final String ID_TYPE = "type";
    public static final String ID_ID = "id";

    private int type;
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
