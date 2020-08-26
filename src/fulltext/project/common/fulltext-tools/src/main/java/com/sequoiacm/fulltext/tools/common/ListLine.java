package com.sequoiacm.fulltext.tools.common;

import java.util.ArrayList;
import java.util.List;

public class ListLine {
    List<String> values = new ArrayList<>();

    public void addItem(String v) {
        values.add(v);
    }

    public int size() {
        return values.size();
    }

    public String getItem(int index) {
        return values.get(index);
    }
}
