package com.sequoiacm.infrastructure.crypto;

public interface ScmCrypto {
    public byte[] encrypt(byte[] orignal, byte[] key) throws Exception;

    public byte[] decrypt(byte[] encrypted, byte[] key) throws Exception;
}
