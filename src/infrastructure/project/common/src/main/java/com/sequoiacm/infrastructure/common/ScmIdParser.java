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

    public static void main(String[] args) {
        ScmIdParser idP = new ScmIdParser("59843fda000001000029091b");
        long m = (long) idP.getSecond() * 1000;
        Date d = new Date(1501840737833L);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        System.out.println(m);
        System.out.println(sdf.format(d));
        System.out.println(1501840737833L - 1501839322000L);
        //1501839322000
        // 2017-08-04-17:35:22

    }
}
