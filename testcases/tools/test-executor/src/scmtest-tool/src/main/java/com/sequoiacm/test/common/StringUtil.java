package com.sequoiacm.test.common;


import java.util.Arrays;
import java.util.List;

public class StringUtil {

    public static List<String> string2List(String source, String separator) {
        return Arrays.asList(source.split(separator));
    }

    public static String subStringAfter(String source, String str) {
        int beginIndex = source.indexOf(str) + str.length();
        if (beginIndex < source.length()) {
            return source.substring(beginIndex);
        }
        return "";
    }

    public static String subStringBefore(String source, String str) {
        int endIndex = source.indexOf(str);
        if (endIndex != -1) {
            return source.substring(0, endIndex);
        }
        return "";
    }

    public static boolean isPositiveInteger(String str) {
        int value;
        try {
            value = Integer.parseInt(str);
        }
        catch (NumberFormatException e) {
            return false;
        }
        return value > 0;
    }
}
