package com.sequoiacm.transfertask;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.element.ScmSiteInfo;
import com.sequoiacm.testcommon.ScmTestTools;
import org.apache.log4j.Logger;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

/**
 * 通过主中心执行迁移任务
 *
 * @author linyoubin
 *
 */
public class CreateTaskInMainSite extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(CreateTaskInMainSite.class);
    private ScmSession mainSiteSession;

    @BeforeClass
    public void setUp() throws ScmException {

        mainSiteSession = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(),
                        getScmUser(), getScmPasswd()));
    }

    @Test
    public void createTaskInMainSite() throws ScmException {
        ScmSiteInfo site2 = ScmTestTools.getSiteInfo(mainSiteSession, getSiteId2());
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), mainSiteSession);
        ScmSystem.Task.startTransferTask(ws, new BasicBSONObject(), ScmType.ScopeType.SCOPE_ALL,
                site2.getName());

    }

    @AfterClass
    public void tearDown() throws ScmException {
        mainSiteSession.close();
    }
}
