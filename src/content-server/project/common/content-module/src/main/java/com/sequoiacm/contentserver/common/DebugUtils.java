package com.sequoiacm.contentserver.common;

public class DebugUtils {
    public static void printHexString(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }

            System.out.print(hex.toUpperCase());
        }

        System.out.println();
    }
}
