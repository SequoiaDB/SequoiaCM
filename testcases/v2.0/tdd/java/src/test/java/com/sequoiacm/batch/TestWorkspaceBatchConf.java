package com.sequoiacm.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.sequoiacm.testcommon.ScmTestTools;

public class TestWorkspaceBatchConf extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkspaceBatchConf.class);
    private ScmSession ss;
    private String wsName = TestWorkspaceBatchConf.class.getSimpleName();

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
    }

    @Test
    public void testAttachRepeat() throws ScmException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setName(wsName);
        conf.setDescription(this.getClass().getName());
        conf.setMetaLocation(new ScmSdbMetaLocation("rootSite", ScmShardingType.MONTH, "domain1"));
        conf.addDataLocation(new ScmSdbDataLocation("rootSite", "domain2", ScmShardingType.YEAR,
                ScmShardingType.QUARTER));
        conf.setBatchFileNameUnique(true);
        conf.setBatchIdTimePattern("yyyyMMdd");
        conf.setBatchIdTimeRegex(".*");
        conf.setBatchShardingType(ScmShardingType.YEAR);

        ScmWorkspace ws = ScmFactory.Workspace.createWorkspace(ss, conf);
        Assert.assertEquals(ws.getBatchIdTimePattern(), "yyyyMMdd");
        Assert.assertEquals(ws.getBatchIdTimeRegex(), ".*");
        Assert.assertEquals(ws.getBatchShardingType(), ScmShardingType.YEAR);
        Assert.assertEquals(ws.isBatchFileNameUnique(), true);

        ws = ScmFactory.Workspace.getWorkspace(wsName, ss);
        Assert.assertEquals(ws.getBatchIdTimePattern(), "yyyyMMdd");
        Assert.assertEquals(ws.getBatchIdTimeRegex(), ".*");
        Assert.assertEquals(ws.getBatchShardingType(), ScmShardingType.YEAR);
        Assert.assertEquals(ws.isBatchFileNameUnique(), true);

        ScmCursor<ScmWorkspaceInfo> c = ScmFactory.Workspace.listWorkspace(ss);
        while (c.hasNext()) {
            ScmWorkspaceInfo wsInfo = c.getNext();
            if (wsInfo.getName().equals(wsName)) {
                Assert.assertEquals(wsInfo.getBatchIdTimePattern(), "yyyyMMdd");
                Assert.assertEquals(wsInfo.getBatchIdTimeRegex(), ".*");
                Assert.assertEquals(wsInfo.getBatchShardingType(), ScmShardingType.YEAR);
                Assert.assertEquals(wsInfo.isBatchFileNameUnique(), true);
            }
        }
        c.close();
        ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);

        conf = new ScmWorkspaceConf();
        conf.setName(wsName);
        conf.setDescription(this.getClass().getName());
        conf.setMetaLocation(new ScmSdbMetaLocation("rootSite", ScmShardingType.MONTH, "domain1"));
        conf.addDataLocation(new ScmSdbDataLocation("rootSite", "domain2", ScmShardingType.YEAR,
                ScmShardingType.QUARTER));
        conf.setBatchFileNameUnique(true);
        conf.setBatchIdTimePattern("yyyyMMdd");
        conf.setBatchIdTimeRegex(".*");
        conf.setBatchShardingType(ScmShardingType.NONE);
        try {
            ScmFactory.Workspace.createWorkspace(ss, conf);
            Assert.fail();
        }
        catch (ScmException e) {
            System.out.println(e.getMessage());
            if (e.getError() != ScmError.INVALID_ARGUMENT) {
                throw e;
            }
        }
        conf.setBatchIdTimeRegex(null);
        conf.setBatchShardingType(ScmShardingType.YEAR);
        try {
            ScmFactory.Workspace.createWorkspace(ss, conf);
            Assert.fail();
        }
        catch (ScmException e) {
            System.out.println(e.getMessage());
            if (e.getError() != ScmError.INVALID_ARGUMENT) {
                throw e;
            }
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        try {
            ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.WORKSPACE_NOT_EXIST) {
                throw e;
            }
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
