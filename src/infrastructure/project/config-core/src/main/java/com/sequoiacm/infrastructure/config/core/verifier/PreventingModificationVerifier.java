package com.sequoiacm.infrastructure.config.core.verifier;

import java.util.Arrays;
import java.util.List;

public class PreventingModificationVerifier implements ScmConfigPropVerifier {
    private String keyPrefix;
    private List<String> exceptKeyPrefixes;

    public PreventingModificationVerifier(String keyPrefix, String... exceptKeyPrefixes) {
        this.keyPrefix = keyPrefix;
        this.exceptKeyPrefixes = Arrays.asList(exceptKeyPrefixes);
    }

    public PreventingModificationVerifier(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    @Override
    public VerifyResult verifyUpdate(String key, String value) {
        if (exceptKeyPrefixes != null && matchExceptKeyPrefixes(key)) {
            return VerifyResult.getValidRes();
        }
        if (keyPrefix != null && key.startsWith(keyPrefix)) {
            return VerifyResult.createInvalidRes(
                    "not allow to modify this property:key=" + key + ",value=" + value);
        }
        return VerifyResult.getUnrecognizedRes();
    }

    private boolean matchExceptKeyPrefixes(String key) {
        if (exceptKeyPrefixes != null) {
            for (String exceptKeyPrefix : exceptKeyPrefixes) {
                if (key.startsWith(exceptKeyPrefix) && !key.equals(exceptKeyPrefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public VerifyResult verifyDeletion(String key) {
        if (exceptKeyPrefixes != null && matchExceptKeyPrefixes(key)) {
            return VerifyResult.getValidRes();
        }
        if (keyPrefix != null && key.startsWith(keyPrefix)) {
            return VerifyResult.createInvalidRes("not allow to delete this property:key=" + key);
        }
        return VerifyResult.getUnrecognizedRes();
    }
}
