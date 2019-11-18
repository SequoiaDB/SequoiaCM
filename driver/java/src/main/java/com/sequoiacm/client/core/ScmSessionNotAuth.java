package com.sequoiacm.client.core;

import com.sequoiacm.client.exception.ScmException;

class ScmSessionNotAuth extends ScmRestSession {

    ScmSessionNotAuth(String url, ScmRequestConfig requestConfig) throws ScmException {
        super(url, requestConfig);
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
