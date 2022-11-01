package com.sequoiacm.infrastructure.common;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SignatureUtils {

    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String SHA_256 = "SHA-256";
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public static String signatureCalculation(String password, String salt, String date) {
        String hashpw = Bcrypt.hashpw(password, salt);
        String message = sha256(hashpw);
        return hmacSHA256(date, message);
    }

    public static boolean signatureChecks(String signature, String password, String date) {
        String message = SignatureUtils.sha256(password);
        String result = SignatureUtils.hmacSHA256(date, message);
        return signature.equals(result);
    }

    public static String hmacSHA256(String secret, String message) {
        Mac hmacSha256 = null;
        try {
            hmacSha256 = Mac.getInstance(HMAC_SHA_256);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(
                    "Failed to get MessageDigest instance: " + HMAC_SHA_256);
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), HMAC_SHA_256);
        try {
            hmacSha256.init(secretKeySpec);
        }
        catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Failed to init secretKeySpec");
        }
        byte[] bytes = hmacSha256.doFinal(message.getBytes());
        return byteArrayToHexString(bytes);
    }

    public static String sha256(String message) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance(SHA_256);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Failed to get MessageDigest instance: " + SHA_256);
        }
        messageDigest.update(message.getBytes(UTF_8));
        return byteArrayToHexString(messageDigest.digest());
    }

    private static String byteArrayToHexString(byte[] b) {
        StringBuilder s = new StringBuilder();
        String tmp;
        for (int n = 0; b != null & n < b.length; n++) {
            tmp = Integer.toHexString(b[n] & 0XFF);
            if (tmp.length() == 1) {
                s.append('0');
            }
            s.append(tmp);
        }
        return s.toString().toLowerCase();
    }
}
