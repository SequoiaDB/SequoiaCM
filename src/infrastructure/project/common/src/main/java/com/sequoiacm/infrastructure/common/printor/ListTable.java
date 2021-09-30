package com.sequoiacm.infrastructure.common.printor;

import java.util.ArrayList;
import java.util.List;

public class ListTable {
    List<ListLine> values = new ArrayList<ListLine>();

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
