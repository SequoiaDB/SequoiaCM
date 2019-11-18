package com.sequoiacm.workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class DeleteWorkspaceInMainSite extends ScmTestMultiCenterBase {
    private static final Logger logger = LoggerFactory.getLogger(DeleteWorkspaceInMainSite.class);
    private ScmSession ss;
    private String wsName = "DeleteWorkspace";

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);
    }

    @Test
    public void test() throws Exception {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setName(wsName);
        conf.setDescription(this.getClass().getName());
        conf.setMetaLocation(new ScmSdbMetaLocation("rootSite", ScmShardingType.MONTH, "domain1"));
        conf.addDataLocation(new ScmSdbDataLocation("rootSite", "domain2", ScmShardingType.YEAR,
                ScmShardingType.QUARTER));
        conf.addDataLocation(new ScmSdbDataLocation("branchSite1", "domain2",
                ScmShardingType.QUARTER, ScmShardingType.MONTH));
        conf.addDataLocation(new ScmSdbDataLocation("branchSite2", "domain2", ScmShardingType.MONTH,
                ScmShardingType.YEAR));

        ScmFactory.Workspace.createWorkspace(ss, conf);

        ScmFactory.Workspace.deleteWorkspace(ss, wsName);

        tryGetWs(ss);

        ScmSession s2 = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        tryGetWs(s2);
        s2.close();

        ScmSession s3 = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer3().getUrl(), getScmUser(), getScmPasswd()));
        tryGetWs(s3);
        s3.close();

    }

    private void tryGetWs(ScmSession ss) throws Exception {
        try {
            ScmFactory.Workspace.getWorkspace(wsName, ss);
            Assert.fail("workspace exist!" + wsName);
        }
        catch (ScmException e) {
            if (e.getError() == ScmError.WORKSPACE_NOT_EXIST) {
                return;
            }
            throw e;
        }
    }

    @AfterClass
    public void cleanUp() throws ScmException {
        try {
            ScmFactory.Workspace.deleteWorkspace(ss, this.getClass().getName(), true);
        }
        finally {
            ss.close();
        }
    }
}
