package com.sequoiacm.infrastructure.config.client.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.verifier.ScmConfigPropVerifier;
import com.sequoiacm.infrastructure.config.core.verifier.VerifyResult;

@Component
public class ScmConfPropVerifiersMgr {
    // ordered list
    private List<ScmConfigPropVerifier> verifiers = new ArrayList<>();

    @Autowired
    public ScmConfPropVerifiersMgr(CommonSpringConfVerifier commonSpringConfVerifier) {
        verifiers.add(commonSpringConfVerifier);
    }

    public void addVerifier(ScmConfigPropVerifier verifier) {
        verifiers.add(verifier);
    }

    public void checkProps(Map<String, String> updateProps, List<String> deleteProps,
            boolean acceptUnrecognized) throws ScmConfigException {
        for (Entry<String, String> entry : updateProps.entrySet()) {
            checkKey(entry.getKey());
            boolean isUnreognized = true;
            for (ScmConfigPropVerifier verifier : verifiers) {
                VerifyResult res = verifier.verifyUpdate(entry.getKey(), entry.getValue());
                if (res.getType() == VerifyResult.VerifyEnum.INVALID) {
                    throw new ScmConfigException(ScmConfError.INVALID_ARG,
                            "invalid argument:" + entry.getKey() + "=" + entry.getValue()
                                    + ",error=" + res.getMessage());
                }

                if (res.getType() == VerifyResult.VerifyEnum.VALID) {
                    isUnreognized = false;
                    break;
                }
            }

            if (isUnreognized && !acceptUnrecognized) {
                throw new ScmConfigException(ScmConfError.INVALID_ARG,
                        "unrecognized config properties:" + entry.getKey() + "="
                                + entry.getValue());
            }
        }

        for (String deletedKey : deleteProps) {
            checkKey(deletedKey);
            boolean isUnreognized = true;
            for (ScmConfigPropVerifier verifier : verifiers) {
                VerifyResult res = verifier.verifyDeletion(deletedKey);
                if (res.getType() == VerifyResult.VerifyEnum.INVALID) {
                    throw new ScmConfigException(ScmConfError.INVALID_ARG,
                            "invalid argument:" + deletedKey + ",error=" + res.getMessage());
                }

                if (res.getType() == VerifyResult.VerifyEnum.VALID) {
                    isUnreognized = false;
                    break;
                }
            }

            if (isUnreognized && !acceptUnrecognized) {
                throw new ScmConfigException(ScmConfError.INVALID_ARG,
                        "unrecognized config properties:" + deletedKey);
            }
        }
    }

    private void checkKey(String key) throws ScmConfigException {
        if (key == null || key.isEmpty() || key.contains("=")) {
            throw new ScmConfigException(ScmConfError.INVALID_ARG, "invalid config key:" + key);
        }
    }
}
