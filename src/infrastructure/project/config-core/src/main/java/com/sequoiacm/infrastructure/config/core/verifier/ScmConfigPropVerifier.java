package com.sequoiacm.infrastructure.config.core.verifier;

public interface ScmConfigPropVerifier {
    // judge whether the property is valid for modify.
    VerifyResult verifyUpdate(String key, String value);

    // judge whether the property is valid for delete.
    VerifyResult verifyDeletion(String key);
}
