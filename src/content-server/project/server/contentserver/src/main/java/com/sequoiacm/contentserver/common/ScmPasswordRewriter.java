package com.sequoiacm.contentserver.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScmPasswordRewriter {
    private static final Logger logger = LoggerFactory.getLogger(ScmPasswordRewriter.class);
    private static ScmPasswordRewriter rewiter = new ScmPasswordRewriter();
    private ScmPasswordRewriter() {
    }

    public static ScmPasswordRewriter getInstance() {
        return rewiter;
    }

    public void rewriteSiteTable() {
    }
}
