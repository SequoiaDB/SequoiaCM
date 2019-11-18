package com.sequoiacm.infrastructure.audit;

import com.sequoiacm.infrastructure.config.core.verifier.ScmConfigPropVerifier;
import com.sequoiacm.infrastructure.config.core.verifier.VerifyResult;

public class ScmAuditPropsVerifier implements ScmConfigPropVerifier {
    @Override
    public VerifyResult verifyUpdate(String key, String value) {
        String userType = ScmAuditDefine.AuditConf.AUDIT_USERTYPE + ".";
        String user = ScmAuditDefine.AuditConf.AUDIT_USER + ".";
        if (key.startsWith(userType)
                && ScmUserAuditType.getScmUserAuditType(key.substring(userType.length())) != null) {
            return checkAduitType(value);
        }
        else if (key.startsWith(user) && key.substring(user.length()).trim().length() != 0) {
            return checkAduitType(value);
        }
        else {
            return checkParameterString(key, value);
        }
    }

    public VerifyResult checkParameterString(String key, String value) {
        switch (key) {
            case ScmAuditDefine.AuditConf.AUDIT_USERMASK:
                return checkUserType(value);
            case ScmAuditDefine.AuditConf.AUDIT_MASK:
                return checkAduitType(value);
            default:
                return VerifyResult.getUnrecognizedRes();
        }
    }

    public VerifyResult checkAduitType(String value) {
        String[] values = value.split("\\|");
        if (value.trim().length() == 0) {
            return VerifyResult.getValidRes();
        }
        for (String mask : values) {
            ScmAuditType maskType = ScmAuditType.getScmAuditType(mask.trim());
            if (maskType == null) {
                return VerifyResult.createInvalidRes("unknown mask:" + mask);
            }
        }
        return VerifyResult.getValidRes();
    }

    public VerifyResult checkUserType(String value) {
        String[] values = value.split("\\|");
        if (value.trim().length() == 0) {
            return VerifyResult.getValidRes();
        }
        for (String userMask : values) {
            ScmUserAuditType userMaskType = ScmUserAuditType.getScmUserAuditType(userMask.trim());
            if (userMaskType == null) {
                return VerifyResult.createInvalidRes("unknown user mask:" + userMask);
            }
        }
        return VerifyResult.getValidRes();
    }

    @Override
    public VerifyResult verifyDeletion(String key) {
        if (key.startsWith(ScmAuditDefine.AuditConf.AUDIT_USERTYPE)
                || key.startsWith(ScmAuditDefine.AuditConf.AUDIT_USER)) {
            return VerifyResult.getValidRes();
        }

        if (key.equals(ScmAuditDefine.AuditConf.AUDIT_MASK)
                || key.equals(ScmAuditDefine.AuditConf.AUDIT_USERMASK)) {
            return VerifyResult.getValidRes();
        }

        return VerifyResult.getUnrecognizedRes();
    }

}
