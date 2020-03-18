package com.sequoiacm.infrastructure.security.sign;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SignUtil {
    

    public static String sign(String algorithm, String secretKey, List<String> stringToSign) {
        if (stringToSign == null) {
            throw new RuntimeException("failed to caculate signature, stringToSign is null");
        }
        byte[] keyBytes = secretKey.getBytes();
        for (String s : stringToSign) {
            keyBytes = sign(s, keyBytes, algorithm);
        }
        return toHex(keyBytes);
    }

    private static byte[] sign(String stringData, byte[] key, String algorithm) {
        try {
            byte[] data = stringData.getBytes("UTF-8");
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key, algorithm));
            return mac.doFinal(data);
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to calculate a request signature: " + e.getMessage(),
                    e);
        }
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i]);
            if (hex.length() == 1) {
                // Append leading zero.
                sb.append("0");
            }
            else if (hex.length() == 8) {
                // Remove ff prefix from negative numbers.
                hex = hex.substring(6);
            }
            sb.append(hex);
        }
        return sb.toString().toLowerCase(Locale.getDefault());
    }

    public static void main(String[] args) {
        sign("sadasd", "dadasd", Arrays.asList(new String[] { "a" }));
    }
}
