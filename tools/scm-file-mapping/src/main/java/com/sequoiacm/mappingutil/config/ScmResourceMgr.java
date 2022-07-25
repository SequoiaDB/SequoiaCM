package com.sequoiacm.mappingutil.config;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.infrastructure.tool.element.ScmUserInfo;
import com.sequoiacm.infrastructure.tool.exception.ScmToolsException;
import com.sequoiacm.mappingutil.exception.ScmExitCode;

import java.util.List;

public class ScmResourceMgr {

    private static volatile ScmResourceMgr instance = null;
    private ScmSessionMgr sessionMgr;

    private ScmResourceMgr() {

    }

    public static ScmResourceMgr getInstance() {
        if (instance == null) {
            synchronized (ScmResourceMgr.class) {
                if (instance == null) {
                    instance = new ScmResourceMgr();
                }
            }
        }

        return instance;
    }

    public void init(List<String> urlList, ScmUserInfo userInfo) throws ScmToolsException {
        try {
            ScmConfigOption config = new ScmConfigOption(urlList, userInfo.getUsername(),
                    userInfo.getPassword());
            ScmSessionPoolConf sessionPoolConf = ScmSessionPoolConf.builder()
                    .setSessionConfig(config).get();
            this.sessionMgr = ScmFactory.Session.createSessionMgr(sessionPoolConf);
        }
        catch (ScmException e) {
            throw new ScmToolsException("Failed to init sessionMgr", ScmExitCode.SYSTEM_ERROR, e);
        }
    }

    public ScmSession getSession() throws ScmToolsException {
        try {
            return sessionMgr.getSession();
        }
        catch (ScmException e) {
            if (e.getError() == ScmError.HTTP_UNAUTHORIZED) {
                throw new ScmToolsException("The username or password is incorrect",
                        ScmExitCode.BAD_CREDENTIAL, e);
            }
            throw new ScmToolsException("Failed to get session", ScmExitCode.SYSTEM_ERROR, e);
        }
    }
}
