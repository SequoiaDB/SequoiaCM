package com.sequoiacm.infrastructure.config.core.verifier;

public class PreventingModificationVerifier implements ScmConfigPropVerifier {
    private String keyPrefix;

    public PreventingModificationVerifier(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    @Override
    public VerifyResult verifyUpdate(String key, String value) {
        if (key.startsWith(keyPrefix)) {
            return VerifyResult.createInvalidRes(
                    "not allow to modify this property:key=" + key + ",value=" + value);
        }
        return VerifyResult.getUnrecognizedRes();
    }

    @Override
    public VerifyResult verifyDeletion(String key) {
        if (key.startsWith(keyPrefix)) {
            return VerifyResult.createInvalidRes("not allow to delete this property:key=" + key);
        }
        return VerifyResult.getUnrecognizedRes();
    }

}
