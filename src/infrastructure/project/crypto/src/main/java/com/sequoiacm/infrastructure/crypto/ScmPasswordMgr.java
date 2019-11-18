package com.sequoiacm.infrastructure.crypto;

import java.util.Random;

public class ScmPasswordMgr {

    public final static int SCM_CRYPT_TYPE_NONE = 0;
    public final static int SCM_CRYPT_TYPE_DES = 1;

    private final int SCM_BASEKEY_LENGTH = 2;
    private final int SCM_KEY_LENGTH = 8;

    private static ScmPasswordMgr instance = new ScmPasswordMgr();

    private ScmPasswordMgr() {
    }

    public static ScmPasswordMgr getInstance() {
        return instance;
    }

    public String encrypt(int cryptType, String orignal) throws Exception {
        if (cryptType == SCM_CRYPT_TYPE_NONE) {
            return orignal;
        }

        ScmCrypto crypto = null;
        if (cryptType == SCM_CRYPT_TYPE_DES) {
            crypto = new ScmDesCrypto();

            byte[] baseKey = generateBaseKey();
            byte[] key = generateKey(baseKey);

            byte[] encrypted = crypto.encrypt(orignal.getBytes(), key);

            StringBuilder sb = new StringBuilder();
            sb.append(ScmCryptoHelper.bytesToHexStr(baseKey));
            sb.append(ScmCryptoHelper.bytesToHexStr(encrypted));
            return sb.toString();
        }

        throw new Exception("invalid crypt type:type=" + cryptType);
    }

    public String decrypt(int cryptType, String encrypted) throws Exception {
        if (cryptType == SCM_CRYPT_TYPE_NONE) {
            return encrypted;
        }

        int length = encrypted.length();
        ScmCrypto crypto = null;
        if (cryptType == SCM_CRYPT_TYPE_DES) {
            int baseKeyStrLen = SCM_BASEKEY_LENGTH * 2;
            if (length < baseKeyStrLen) {
                throw new Exception("encrypted str is invalid:str=" + encrypted);
            }

            crypto = new ScmDesCrypto();
            byte[] baseKey = ScmCryptoHelper.hexStrToBytes(encrypted.substring(0, baseKeyStrLen));
            byte[] key = generateKey(baseKey);

            byte[] orignalEncrypted = ScmCryptoHelper
                    .hexStrToBytes(encrypted.substring(baseKeyStrLen));
            byte[] result = crypto.decrypt(orignalEncrypted, key);

            return new String(result);
        }

        throw new Exception("invalid crypt type:type=" + cryptType);
    }

    private byte[] generateKey(byte[] baseKey) {
        byte[] key = new byte[SCM_KEY_LENGTH];

        int j = 0;
        for (int i = 0; i < key.length; i++) {
            if (j >= baseKey.length) {
                j = 0;
            }

            key[i] = baseKey[j];
            j++;
        }

        return key;
    }

    private byte[] generateBaseKey() {
        Random r = new Random();
        byte[] b = new byte[SCM_BASEKEY_LENGTH];
        r.nextBytes(b);

        return b;
    }

    public static void main(String[] args) throws Exception {
        ScmPasswordMgr spm = ScmPasswordMgr.getInstance();
        String original = "sdbadmin";
        String encrypted = spm.encrypt(1, original);
        System.out.println(encrypted);
        System.out.println(spm.decrypt(1, encrypted));
    }
}
