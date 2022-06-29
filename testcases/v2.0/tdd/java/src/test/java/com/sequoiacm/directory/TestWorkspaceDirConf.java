package com.sequoiacm.directory;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestWorkspaceDirConf extends ScmTestMultiCenterBase {
    private ScmSession ss;
    private String wsName = "TestWsDirConf";

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
    }

    @Test
    public void test() throws ScmException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setName(wsName);
        conf.setDescription(this.getClass().getName());
        conf.setMetaLocation(new ScmSdbMetaLocation("rootSite", ScmShardingType.MONTH, "domain1"));
        conf.addDataLocation(new ScmSdbDataLocation("rootSite", "domain2", ScmShardingType.YEAR,
                ScmShardingType.QUARTER));
        conf.setEnableDirectory(false);
        ScmWorkspace ws = ScmFactory.Workspace.createWorkspace(ss, conf);
        Assert.assertEquals(ws.isEnableDirectory(), false);
        checkWs(false);
        ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);

        conf = new ScmWorkspaceConf();
        conf.setName(wsName);
        conf.setDescription(this.getClass().getName());
        conf.setMetaLocation(new ScmSdbMetaLocation("rootSite", ScmShardingType.MONTH, "domain1"));
        conf.addDataLocation(new ScmSdbDataLocation("rootSite", "domain2", ScmShardingType.YEAR,
                ScmShardingType.QUARTER));
        conf.setEnableDirectory(true);
        ws = ScmFactory.Workspace.createWorkspace(ss, conf);
        Assert.assertEquals(ws.isEnableDirectory(), true);
        checkWs(true);
        ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);

        conf = new ScmWorkspaceConf();
        conf.setName(wsName);
        conf.setDescription(this.getClass().getName());
        conf.setMetaLocation(new ScmSdbMetaLocation("rootSite", ScmShardingType.MONTH, "domain1"));
        conf.addDataLocation(new ScmSdbDataLocation("rootSite", "domain2", ScmShardingType.YEAR,
                ScmShardingType.QUARTER));
        ws = ScmFactory.Workspace.createWorkspace(ss, conf);
        Assert.assertFalse(ws.isEnableDirectory());
        checkWs(false);

    }

    private void checkWs(boolean isEnable) throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsName, ss);
        Assert.assertEquals(ws.isEnableDirectory(), isEnable);

        ScmCursor<ScmWorkspaceInfo> c = ScmFactory.Workspace.listWorkspace(ss);
        boolean isChecked = false;
        while (c.hasNext()) {
            ScmWorkspaceInfo wInfo = c.getNext();
            if (wInfo.getName().equals(wsName)) {
                Assert.assertEquals(wInfo.isEnableDirectory(), isEnable);
                isChecked = true;
            }
        }
        c.close();
        Assert.assertTrue(isChecked);
    }

    @AfterClass
    public void clear() throws ScmException {
        try {
            ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.WORKSPACE_NOT_EXIST) {
                throw e;
            }
        }
        ss.close();
    }
}
