package com.sequoiacm.om.omserver.common;

import java.util.ArrayList;
import java.util.List;

public class PageUtil {

    public static <T> List<T> getPageOfResult(List<T> objectList, long skip, int limit) {
        List<T> res = new ArrayList<>();
        int counter = 0;
        for (T obj : objectList) {
            if (limit != -1 && limit == res.size()) {
                break;
            }
            if (++counter > skip) {
                res.add(obj);
            }
        }
        return res;

    }
}
