package com.sequoiacm.infrastructure.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ScmIdParser {
    private ScmParesedId paresedId;

    public ScmIdParser(String id) throws IllegalArgumentException {
        try {
            paresedId = ScmIdGenerator.FileId.parseString(id);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("invalid id string:" + id, e);
        }
    }

    public long getSecond() {
        return paresedId.getSeconds();
    }

    public String getMonth() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        Date date = new Date((long) paresedId.getSeconds() * 1000);
        return sdf.format(date);
    }

}
