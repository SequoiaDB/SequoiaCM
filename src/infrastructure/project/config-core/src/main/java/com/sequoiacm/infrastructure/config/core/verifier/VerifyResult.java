package com.sequoiacm.infrastructure.config.core.verifier;

public class VerifyResult {
    private static VerifyResult valid = new VerifyResult(VerifyEnum.VALID, null);
    private static VerifyResult unrecognized = new VerifyResult(VerifyEnum.UNRECOGNIZED, null);

    public static VerifyResult getValidRes() {
        return valid;
    }

    public static VerifyResult createInvalidRes(String invalidMessage) {
        return new VerifyResult(VerifyEnum.INVALID, invalidMessage);
    }

    public static VerifyResult getUnrecognizedRes() {
        return unrecognized;
    }

    private VerifyEnum type;
    private String message;

    private VerifyResult(VerifyEnum type, String message) {
        this.type = type;
        this.message = message;
    }

    public VerifyEnum getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public enum VerifyEnum {
        VALID,
        INVALID,
        UNRECOGNIZED;
    }
}
