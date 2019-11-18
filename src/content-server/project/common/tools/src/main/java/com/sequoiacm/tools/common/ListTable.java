package com.sequoiacm.tools.common;

import java.util.ArrayList;
import java.util.List;

public class ListTable {
    List<ListLine> values = new ArrayList<>();

    public void addLine(ListLine line) {
        values.add(line);
    }

    public int size() {
        return values.size();
    }

    public ListLine getLine(int index) {
        return values.get(index);
    }
}
