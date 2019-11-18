package com.sequoiacm.infrastructure.crypto;

public class ScmCryptoHelper {
    public static String bytesToHexStr(byte[] b) {
        if (null == b) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            int x = b[i] & 0xFF;
            String s = Integer.toHexString(x).toUpperCase();
            if (s.length() == 1) {
                buf.append("0");
            }

            buf.append(s);
        }

        return buf.toString();
    }

    public static byte[] hexStrToBytes(String s) throws Exception {
        final int len = s.length();
        if (len % 2 != 0) {
            throw new Exception("input is not a avaliable hex string:s=" + s);
        }

        int length = s.length() / 2;
        byte b[] = new byte[length];
        for (int i = 0; i < b.length; i++) {
            String tmp = s.substring(i * 2, i * 2 + 2);
            if (!isHexStr(tmp)) {
                throw new Exception("input is not a avaliable hex string:s=" + s);
            }

            b[i] = (byte) Integer.parseInt(tmp, 16);
        }

        return b;
    }

    private static boolean isHexStr(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                continue;
            }

            if (c >= 'a' && c <= 'f') {
                continue;
            }

            if (c >= 'A' && c <= 'F') {
                continue;
            }

            return false;
        }

        return true;
    }
}
