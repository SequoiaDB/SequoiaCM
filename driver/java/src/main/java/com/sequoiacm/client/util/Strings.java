package com.sequoiacm.client.util;

import java.util.List;

public final class Strings {
    private Strings() {
    }

    public static boolean isEmpty(Object str) {
        return (str == null || "".equals(str));
    }

    public static boolean hasLength(String str) {
        return (str != null && !str.isEmpty());
    }

    public static boolean hasText(String str) {
        return (hasLength(str) && containsText(str));
    }

    private static boolean containsText(CharSequence str) {
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static String join(List<String> list, String delimiter) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s).append(delimiter);
        }
        return sb.substring(0, sb.length() - 1);
    }
}
