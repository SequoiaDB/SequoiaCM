package com.sequoiacm.infrastructure.security.sign;

import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

public class SignUtil {

    public static String sign(String algorithm, String secretKey, List<String> stringToSign,
                              String signatrueEncoder) {
        if (stringToSign == null) {
            throw new RuntimeException("failed to caculate signature, stringToSign is null");
        }
        byte[] keyBytes = secretKey.getBytes();
        for (String s : stringToSign) {
            keyBytes = sign(s, keyBytes, algorithm);
        }
        if (signatrueEncoder.equals("base16")) {
            return toHex(keyBytes);
        }
        if (signatrueEncoder.equals("base64")) {
            return base64(keyBytes);
        }
        throw new IllegalArgumentException("unknown signatrue encoder:" + signatrueEncoder);
    }

    public static String base64(byte[] keyBytes) {
        return DatatypeConverter.printBase64Binary(keyBytes);
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

    public static String toHex(byte[] data) {
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

    public static byte[] hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes("UTF-8"));
            return md.digest();
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to compute hash: " + e.getMessage(), e);
        }
    }

    public static String toHex(String base64Str) {
        return Hex.encodeHexString(Base64.decodeBase64(base64Str));
    }

    public static String toBase64(String hexStr) {
        try {
            return Base64.encodeBase64String(Hex.decodeHex(hexStr.toCharArray()));
        }
        catch (Exception e) {
            throw new RuntimeException("failed to decode string:" + hexStr, e);
        }
    }

    public static String calcHexMd5(String content) {
        try {
            MessageDigest md5Calc = MessageDigest.getInstance("MD5");
            md5Calc.update(content.getBytes());
            byte[] md5Bytes = md5Calc.digest();
            return Hex.encodeHexString(md5Bytes);
        }
        catch (Exception e) {
            throw new RuntimeException("failed to calc md5 string:" + content, e);
        }
    }

    public static void main(String[] args) {
        System.out.println(SignUtil.toHex(hash("PUT\n" + "/s3/bucket1/ExampleObject.txt\n" + "\n"
                + "content-length:857\n" + "host:192.168.10.95:8080\n"
                + "x-amz-content-sha256:8a816cbbd9f0a0ebf8c5196c255b64c7cddec8ea12a46c60abeb3b2eda193887\n"
                + "x-amz-date:20200407T023705Z\n" + "x-amz-storage-class:REDUCED_REDUNDANCY\n"
                + "\n" + "content-length;host;x-amz-content-sha256;x-amz-date;x-amz-storage-class\n"
                + "8a816cbbd9f0a0ebf8c5196c255b64c7cddec8ea12a46c60abeb3b2eda193887")));
        System.out.println(SignUtil.toHex(hash("PUT\n" + "/s3/bucket1/ExampleObject.txt\n" + "\n"
                + "content-length:857\n" + "host:192.168.10.95\n"
                + "x-amz-content-sha256:8a816cbbd9f0a0ebf8c5196c255b64c7cddec8ea12a46c60abeb3b2eda193887\n"
                + "x-amz-date:20200407T023705Z\n" + "x-amz-storage-class:REDUCED_REDUNDANCY\n"
                + "\n" + "content-length;host;x-amz-content-sha256;x-amz-date;x-amz-storage-class\n"
                + "8a816cbbd9f0a0ebf8c5196c255b64c7cddec8ea12a46c60abeb3b2eda193887")));
    }
}