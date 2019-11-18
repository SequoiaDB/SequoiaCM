package com.sequoiacm.user;

import java.util.UUID;

import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmBatch;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmUser;
import com.sequoiacm.client.core.ScmUserModifier;
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestMonitorRole extends ScmTestMultiCenterBase {
    private static final Logger logger = LoggerFactory.getLogger(TestMonitorRole.class);
    private ScmSession ss;

    @BeforeClass
    public void init() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));

    }

    @Test
    public void test() throws Exception {
        ScmWorkspace adminWs = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        ScmBatch batch = ScmFactory.Batch.createInstance(adminWs);
        batch.setName(UUID.randomUUID().toString());
        batch.save();

        ScmFile file = ScmFactory.File.createInstance(adminWs);
        file.setFileName(UUID.randomUUID().toString());
        file.save();

        try {
            ScmFactory.User.deleteUser(ss, "TestMonitorRole");
        }
        catch (Exception e) {
        }

        ScmUserPasswordType userType = ScmUserPasswordType.LOCAL;
        ScmUser user = ScmFactory.User.createUser(ss, "TestMonitorRole", userType, "pwd");
        ScmRole role = ScmFactory.Role.getRole(ss, "AUTH_MONITOR");

        ScmUserModifier md = new ScmUserModifier();
        md.addRole(role);
        ScmFactory.User.alterUser(ss, user, md);
        Thread.sleep(15000);

        ScmSession monitorSession = ScmFactory.Session.createSession(
                new ScmConfigOption(getServer1().getUrl(), "TestMonitorRole", "pwd"));
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), monitorSession);
        ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance(ws,
                ScopeType.SCOPE_CURRENT, new BasicBSONObject());
        cursor.close();
        ScmFactory.File.countInstance(ws, ScopeType.SCOPE_CURRENT, new BasicBSONObject());
        ScmFactory.Directory.countInstance(ws, new BasicBSONObject());
        ScmFactory.Batch.countInstance(ws, new BasicBSONObject());
        ScmFactory.Batch.listInstance(ws, new BasicBSONObject()).close();

        try {
            ScmFactory.File.getInstance(ws, file.getFileId());
            Assert.fail("monitor role can not get file!!");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.OPERATION_UNAUTHORIZED, e.getMessage());
        }

        try {
            ScmFactory.Batch.getInstance(ws, batch.getId());
            Assert.fail("monitor role can not get batch!!");
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getError(), ScmError.OPERATION_UNAUTHORIZED, e.getMessage());
        }

        file.delete(true);
        batch.delete();
        monitorSession.close();
        ScmFactory.User.deleteUser(ss, "TestMonitorRole");
    }

    @AfterClass
    public void cleanUp() throws ScmException {
        ss.close();
    }

}
