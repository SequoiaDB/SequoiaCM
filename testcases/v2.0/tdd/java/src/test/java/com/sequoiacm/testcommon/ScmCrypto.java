package com.sequoiacm.testcommon;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public interface ScmCrypto {
    public byte[] encrypt(byte[] src, String key);
    public byte[] decrypt(byte[] dest, String key);
}

class ScmDESCrypto implements ScmCrypto {

    private Key getKey(byte[] inputKey) {
        byte[] array = new byte[8];

        for (int i = 0; i < inputKey.length && i < array.length; i++) {
            array[i] = inputKey[i];
        }

        return new SecretKeySpec(array, "DES");
    }

    @Override
    public byte[] encrypt(byte[] src, String key) {
        try {
            Cipher c = Cipher.getInstance("DES");
            Key cKey = getKey(key.getBytes("utf-8"));
            c.init(Cipher.ENCRYPT_MODE, cKey);
            return c.doFinal(src);
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public byte[] decrypt(byte[] dest, String key) {
        try {
            Cipher c = Cipher.getInstance("DES");
            Key cKey = getKey(key.getBytes("utf-8"));
            c.init(Cipher.DECRYPT_MODE, cKey);
            return c.doFinal(dest);
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    public static void main(String []args) throws UnsupportedEncodingException {
        ScmCrypto c = new ScmDESCrypto();
        String key = ScmTestTools.randomString(4);
        String src = ScmTestTools.randomString(10);

        byte[] dest = c.encrypt(src.getBytes("utf-8"), key);

        String result = new Base64().encodeToString(dest);
        System.out.println("key=" + key);
        System.out.println("src=" + src);
        System.out.println("result=" + result);

        byte[] temp = c.decrypt(dest, key);
        String newSrc = new String(temp, "utf-8");
        System.out.println("newSrc=" + newSrc);
    }

}
