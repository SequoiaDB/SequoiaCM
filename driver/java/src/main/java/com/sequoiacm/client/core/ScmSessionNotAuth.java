package com.sequoiacm.client.core;

import com.sequoiacm.client.exception.ScmException;

class ScmSessionNotAuth extends ScmRestSession {

    ScmSessionNotAuth(String url, ScmRequestConfig requestConfig, String preferredRegion,
            String preferredZone) throws ScmException {
        super(url, requestConfig, preferredRegion, preferredZone);
    }

    @Override
    public String getUser() {
        return "";
    }

    @Override
    public String getSessionId() {
        return "-1";
    }

}
