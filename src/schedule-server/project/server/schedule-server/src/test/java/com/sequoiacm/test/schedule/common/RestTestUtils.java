package com.sequoiacm.test.schedule.common;

import java.security.MessageDigest;

public class RestTestUtils {
    public static String md5(String source) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            return "";
        }
        char[] charArray = source.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++) {
            byteArray[i] = (byte) charArray[i];
        }

        byte[] md5Bytes = md5.digest(byteArray);

        StringBuilder hexValue = new StringBuilder();

        for (byte md5Byte : md5Bytes) {
            int val = md5Byte & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }

        return hexValue.toString();
    }

}
