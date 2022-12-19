package com.sequoiacm.infrastructure.config.client.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequoiacm.infrastructure.config.client.props.ScmConfPropsScanner;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfError;
import com.sequoiacm.infrastructure.config.core.exception.ScmConfigException;
import com.sequoiacm.infrastructure.config.core.verifier.ScmConfigPropVerifier;
import com.sequoiacm.infrastructure.config.core.verifier.VerifyResult;

@Component
public class ScmConfPropVerifiersMgr {
    private static final Logger logger = LoggerFactory.getLogger(ScmConfPropVerifiersMgr.class);
    // ordered list
    private List<ScmConfigPropVerifier> verifiers = new ArrayList<>();
    private boolean isSorted = false;

    @Autowired
    public ScmConfPropVerifiersMgr(CommonSpringConfVerifier commonSpringConfVerifier,
            ScmConfPropsScanner scmConfPropsScanner, ConversionService conversionService) {
        verifiers.add(commonSpringConfVerifier);

        verifiers.add(new ScmConfigPropVerifier() {
            @Override
            public VerifyResult verifyUpdate(String key, String value) {
                if (scmConfPropsScanner.isScmConfProp(key)) {
                    if (value == null) {
                        return VerifyResult.getValidRes();
                    }
                    Class<?> type = scmConfPropsScanner.getType(key);
                    if (type != null) {
                        try {
                            conversionService.convert(value, type);
                        }
                        catch (ConversionFailedException e) {
                            logger.error("failed to check config: config={}, value={}", key, value,
                                    e);
                            return VerifyResult.createInvalidRes("config " + key + "=" + value
                                    + " is invalid: " + e.getMessage());
                        }
                    }
                    return VerifyResult.getValidRes();
                }
                return VerifyResult.getUnrecognizedRes();
            }

            @Override
            public VerifyResult verifyDeletion(String key) {
                return verifyUpdate(key, null);
            }

            @Override
            public int order() {
                return Integer.MAX_VALUE;
            }
        });
    }

    public void addVerifier(ScmConfigPropVerifier verifier) {
        verifiers.add(verifier);
    }

    public void checkProps(Map<String, String> updateProps, List<String> deleteProps,
            boolean acceptUnrecognized) throws ScmConfigException {
        if (!isSorted) {
            sortVerifiers();
        }
        for (Entry<String, String> entry : updateProps.entrySet()) {
            checkKey(entry.getKey());
            boolean isUnrecognized = true;
            for (ScmConfigPropVerifier verifier : verifiers) {
                VerifyResult res = verifier.verifyUpdate(entry.getKey(), entry.getValue());
                if (res.getType() == VerifyResult.VerifyEnum.INVALID) {
                    throw new ScmConfigException(ScmConfError.INVALID_ARG,
                            "invalid argument:" + entry.getKey() + "=" + entry.getValue()
                                    + ",error=" + res.getMessage());
                }

                if (res.getType() == VerifyResult.VerifyEnum.VALID) {
                    isUnrecognized = false;
                    break;
                }
            }

            if (isUnrecognized && !acceptUnrecognized) {
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

    private synchronized void sortVerifiers() {
        Collections.sort(verifiers, new Comparator<ScmConfigPropVerifier>() {
            @Override
            public int compare(ScmConfigPropVerifier o1, ScmConfigPropVerifier o2) {
                return o1.order() - o2.order();
            }
        });
        isSorted = true;
    }

    private void checkKey(String key) throws ScmConfigException {
        if (key == null || key.isEmpty() || key.contains("=")) {
            throw new ScmConfigException(ScmConfError.INVALID_ARG, "invalid config key:" + key);
        }
    }
}
