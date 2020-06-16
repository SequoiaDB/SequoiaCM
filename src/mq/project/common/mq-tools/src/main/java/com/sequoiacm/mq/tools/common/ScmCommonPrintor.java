package com.sequoiacm.mq.tools.common;

import java.util.ArrayList;
import java.util.List;

public class ScmCommonPrintor {
    private static List<Integer> getLengthList(List<String> header, ListTable t) {
        List<Integer> lengthList = new ArrayList<>();
        for (String hName : header) {
            if (null != hName) {
                lengthList.add(hName.length());
            }
            else {
                lengthList.add(4);
            }
        }

        for (int i = 0; i < t.size(); i++) {
            ListLine l = t.getLine(i);

            for (int j = 0; j < l.size(); j++) {
                String item = l.getItem(j);
                if (null != item) {
                    if (j < lengthList.size()) {
                        if (lengthList.get(j) < item.length()) {
                            lengthList.set(j, item.length());
                        }
                    }
                    else {
                        lengthList.add(item.length());
                    }
                }
            }
        }

        return lengthList;
    }

    public static void print(List<String> headerList, ListTable t) {
        List<Integer> lengthList = getLengthList(headerList, t);

        // print header
        for (int i = 0; i < lengthList.size(); i++) {
            String h = "";
            if (i < headerList.size()) {
                h = headerList.get(i);
            }

            int extraSpace = lengthList.get(i) + 1;
            if (null != h) {
                System.out.print(h);
                extraSpace = extraSpace - h.length() + 1;
            }

            if (i != lengthList.size() - 1) {
                ScmCommon.printSpace(extraSpace);
            }
        }
        System.out.println();

        for (int i = 0; i < t.size(); i++) {
            ListLine l = t.getLine(i);

            for (int j = 0; j < l.size(); j++) {
                String item = l.getItem(j);
                if (item == null) {
                    item = "null";
                }
                int extraSpace = lengthList.get(j) + 1;
                if (null != item) {
                    System.out.print(item);
                    extraSpace = extraSpace - item.length() + 1;
                }

                if (j != l.size() - 1) {
                    ScmCommon.printSpace(extraSpace);
                }
            }

            System.out.println();
        }

        System.out.println("Total:" + t.size());
    }

    public static void main(String[] args) {
        ListTable t = new ListTable();
        ListLine l = new ListLine();
        l.addItem("aaaaaa");
        l.addItem("aaaaaabbbbbbbbbbbbb");
        l.addItem("ccc");
        t.addLine(l);
        List<String> headerList = new ArrayList<String>();
        headerList.add("head1");
        headerList.add("head2");
        headerList.add("head3");

        print(headerList, t);
    }
}
