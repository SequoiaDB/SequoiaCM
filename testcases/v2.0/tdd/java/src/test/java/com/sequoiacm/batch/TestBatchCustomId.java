package com.sequoiacm.batch;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmWorkspaceInfo;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.privilege.TestPrivilegeCommon;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;
import com.sequoiadb.base.Sequoiadb;

public class TestBatchCustomId extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestBatchCustomId.class);
    private ScmSession ss;
    private String wsName = TestBatchCustomId.class.getSimpleName();

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
    }

    @Test
    public void testAttachRepeat() throws ScmException, InterruptedException {
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setName(wsName);
        conf.setDescription(this.getClass().getName());
        conf.setMetaLocation(new ScmSdbMetaLocation("rootSite", ScmShardingType.MONTH, "domain1"));
        conf.addDataLocation(new ScmSdbDataLocation("rootSite", "domain2", ScmShardingType.YEAR,
                ScmShardingType.QUARTER));
        conf.setBatchShardingType(ScmShardingType.MONTH);
        conf.setBatchIdTimePattern("yyyyMMdd");
        conf.setBatchIdTimeRegex("(?<=\\w{3}\\.)(\\d{8})(?=\\.\\w{3})");

        ScmWorkspace ws = ScmFactory.Workspace.createWorkspace(ss, conf);
        ScmRole role = ScmFactory.Role.getRole(ss, "ROLE_AUTH_ADMIN");
        ScmFactory.Role.grantPrivilege(ss, role, ScmResourceFactory.createWorkspaceResource(wsName),
                ScmPrivilegeType.ALL);
        Thread.sleep(20000);

        try {
            ScmBatch b = ScmFactory.Batch.createInstance(ws);
            b.setName("test");
            b.save();
            Assert.fail();
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.INVALID_ARGUMENT) {
                throw e;
            }
        }

        ScmBatch batch = ScmFactory.Batch.createInstance(ws, "nih.20200723.jjj");
        batch.setName("test");
        batch.save();

        Sequoiadb sdb = new Sequoiadb(getSdb1().getUrl(), getSdbUser(), getSdbPasswd());
        sdb.getCollectionSpace(ws.getName() + "_META")
                .getCollection("BATCH_" + CommonHelper.getCurrentYearMonth(batch.getCreateTime()));
        sdb.close();
        batch = ScmFactory.Batch.getInstance(ws, batch.getId());
        Assert.assertEquals(batch.getId().get(), "nih.20200723.jjj");
        batch.delete();
        try {
            ScmFactory.Batch.getInstance(ws, batch.getId());
            Assert.fail();
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.BATCH_NOT_FOUND) {
                throw e;
            }
        }
        ScmFactory.Workspace.deleteWorkspace(ss, wsName, true);

        ScmWorkspace normalWs = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        batch = ScmFactory.Batch.createInstance(normalWs, "customid");
        batch.setName("test");
        batch.save();

        batch = ScmFactory.Batch.getInstance(normalWs, batch.getId());
        Assert.assertEquals(batch.getId().get(), "customid");

        ScmFactory.Batch.deleteInstance(normalWs, batch.getId());
        try {
            ScmFactory.Batch.getInstance(normalWs, batch.getId());
            Assert.fail();
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.BATCH_NOT_FOUND) {
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

    public static void main(String[] args) {
        Pattern p = Pattern.compile("(?<=\\w{3}\\.)(\\d{8})(?=\\.\\w{3})");
        Matcher m = p.matcher("nih.20200723.jjj");
        m.find();
        System.out.println(m.group());
    }
}
