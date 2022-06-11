package com.sequoiacm.contentserver.common;

import com.sequoiacm.infrastructure.dispatcher.ScmRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScmRestClientUtils {

    private static ScmRestClient scmRestClient;

    public static ScmRestClient getScmRestClient() {
        return scmRestClient;
    }

    @Autowired
    public void setScmRestClient(ScmRestClient scmRestClient) {
        ScmRestClientUtils.scmRestClient = scmRestClient;
    }
}
