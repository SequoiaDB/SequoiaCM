package com.sequoiacm.infrastructure.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class ScmDesCrypto implements ScmCrypto {

    @Override
    public byte[] encrypt(byte[] orignal, byte[] key) throws Exception {
        return encryptOrDecrypt(orignal, key, true);
    }

    @Override
    public byte[] decrypt(byte[] encrypted, byte[] key) throws Exception {
        return encryptOrDecrypt(encrypted, key, false);
    }

    private byte[] encryptOrDecrypt(byte[] contents, byte[] key, boolean isEncrypt)
            throws Exception {
        try {
            int mode = Cipher.ENCRYPT_MODE;
            if (!isEncrypt) {
                mode = Cipher.DECRYPT_MODE;
            }

            DESKeySpec keySpec = new DESKeySpec(key);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = factory.generateSecret(keySpec);

            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(mode, secretKey, new IvParameterSpec(keySpec.getKey()));
            return cipher.doFinal(contents);
        }
        catch (Exception e) {
            throw new Exception(isEncrypt ? "encrypt"
                    : "decrypt" + " failed:key=" + ScmCryptoHelper.bytesToHexStr(key), e);
        }
    }
}
