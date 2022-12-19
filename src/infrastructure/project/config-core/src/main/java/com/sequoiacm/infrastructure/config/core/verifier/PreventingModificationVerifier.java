package com.sequoiacm.infrastructure.config.core.verifier;

import java.util.Arrays;
import java.util.List;

public class PreventingModificationVerifier implements ScmConfigPropVerifier {
    private List<String> preventingModifiedConfList;

    public PreventingModificationVerifier(String... confArr) {
        this.preventingModifiedConfList = Arrays.asList(confArr);
    }

    @Override
    public VerifyResult verifyUpdate(String key, String value) {
        if (preventingModifiedConfList.contains(key)) {
            return VerifyResult.createInvalidRes(
                    "not allow to modify this property:key=" + key + ",value=" + value);
        }
        return VerifyResult.getUnrecognizedRes();
    }

    @Override
    public VerifyResult verifyDeletion(String key) {
        if (preventingModifiedConfList.contains(key)) {
            return VerifyResult.createInvalidRes("not allow to modify this property:key=" + key);
        }
        return VerifyResult.getUnrecognizedRes();
    }
}
