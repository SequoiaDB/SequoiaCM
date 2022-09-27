package com.sequoiacm.contentserver.bizconfig;

import com.sequoiacm.infrastructure.config.core.verifier.ScmConfigPropVerifier;
import com.sequoiacm.infrastructure.config.core.verifier.VerifyResult;

import java.util.ArrayList;
import java.util.List;

public class ScmJobManagerConfigVerifier implements ScmConfigPropVerifier {

    private List<String> configList = new ArrayList<>();

    public ScmJobManagerConfigVerifier() {
        configList.add("scm.jobManager.threadpool.coreSize");
        configList.add("scm.jobManager.threadpool.maxSize");
    }

    @Override
    public VerifyResult verifyUpdate(String key, String value) {
        if (configList.contains(key)) {
            return VerifyResult.getValidRes();
        }
        return VerifyResult.getUnrecognizedRes();
    }

    @Override
    public VerifyResult verifyDeletion(String key) {
        if (configList.contains(key)) {
            return VerifyResult.getValidRes();
        }
        return VerifyResult.getUnrecognizedRes();
    }
}
