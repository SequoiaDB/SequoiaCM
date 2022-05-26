package com.sequoiacm.contentserver.model;

public class OverwriteOption {


    private boolean isOverwrite;
    private SessionInfoWrapper sessionInfoWrapper;

    private OverwriteOption() {

    }

    public static OverwriteOption doOverwrite(String sessionId, String userDetail) {
        OverwriteOption op = new OverwriteOption();
        op.isOverwrite = true;
        op.sessionInfoWrapper = new SessionInfoWrapper(sessionId, userDetail);
        return op;
    }

    public static OverwriteOption doNotOverwrite() {
        OverwriteOption op = new OverwriteOption();
        op.isOverwrite = false;
        return op;
    }

    public boolean isOverwrite() {
        return isOverwrite;
    }

    public SessionInfoWrapper getSessionInfoWrapper() {
        return sessionInfoWrapper;
    }
}
