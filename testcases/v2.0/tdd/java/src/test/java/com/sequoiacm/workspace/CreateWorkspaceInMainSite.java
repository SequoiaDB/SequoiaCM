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
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class CreateWorkspaceInMainSite extends ScmTestMultiCenterBase {
    private static final Logger logger = LoggerFactory.getLogger(CreateWorkspaceInMainSite.class);
    private ScmSession ss;
    private String wsName = "createWorkspaceTest1";

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

        ScmWorkspace ws = ScmFactory.Workspace.createWorkspace(ss, conf);
        checkWs(conf, ws);

        qeuryAndCheckWorkspace(ss, conf);

        ScmSession s2 = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        qeuryAndCheckWorkspace(s2, conf);
        s2.close();

        ScmSession s3 = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer3().getUrl(), getScmUser(), getScmPasswd()));
        qeuryAndCheckWorkspace(s3, conf);
        s3.close();

    }

    private void qeuryAndCheckWorkspace(ScmSession ss, ScmWorkspaceConf conf) throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, ss);
        checkWs(conf, ws);
    }

    private void checkWs(ScmWorkspaceConf conf, ScmWorkspace ws) {
        Assert.assertEquals(ws.getName(), wsName, ws.toString());
        Assert.assertEquals(ws.getDescription(), this.getClass().getName(), ws.getDescription());

        Assert.assertEquals(ws.getMetaLocation(), conf.getMetaLocation(),
                ws.getMetaLocation().toString());
        Assert.assertEquals(ws.getDataLocations(), conf.getDataLocations(),
                ws.getDataLocations().toString());
        Assert.assertEquals(ws.getCreateUser(), ss.getUser());
        Assert.assertEquals(ws.getUpdateUser(), ss.getUser());
    }

    @AfterClass
    public void cleanUp() throws ScmException {
        try {
            ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);
        }
        finally {
            ss.close();
        }
    }
}
