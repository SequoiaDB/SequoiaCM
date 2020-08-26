package com.sequoiacm.fulltext.server.lock;

import org.springframework.stereotype.Component;

@Component
public class LockPathFactory {
    private static final String FULLTEXT = "fulltext";

    public LockPath fulltextLockPath(String ws) {
        String[] lockPath = { FULLTEXT, ws };
        return new LockPath(lockPath);
    }
}
