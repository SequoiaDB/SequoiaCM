package com.sequoiacm.infrastructure.common;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ScmStringUtil {
    private static Set<Character> regexChar = new HashSet<Character>();
    static {
        Collections.addAll(regexChar, '^', '$', '.', '[', ']', '|', '(', ')', '{', '}', '+', '?');
    }

    public static String wildcardToRegex(String wildcard) {
        StringBuilder regex = new StringBuilder();
        regex.append("^");
        boolean escaped = false;
        for (int i = 0; i < wildcard.length(); i++) {
            char c = wildcard.charAt(i);
            if (escaped) {
                escaped = false;
                if (c == '*' || c == '.' || c == '\\') {
                    regex.append('\\').append(c);
                    continue;
                }
                regex.append("\\\\");
                regex.append(c);
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '*') {
                regex.append(".*");
                continue;
            }

            if (c == '?') {
                regex.append(".");
                continue;
            }

            if (regexChar.contains(c)) {
                regex.append("\\");
            }

            regex.append(c);
        }
        regex.append("$");
        return regex.toString();
    }
}
