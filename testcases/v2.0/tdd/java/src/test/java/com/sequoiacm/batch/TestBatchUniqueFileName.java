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
import com.sequoiacm.client.core.ScmDirectory;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmRole;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.bizconf.ScmSdbDataLocation;
import com.sequoiacm.client.element.bizconf.ScmSdbMetaLocation;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.element.privilege.ScmResourceFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestBatchUniqueFileName extends ScmTestMultiCenterBase {

    private final static Logger logger = LoggerFactory.getLogger(TestBatchUniqueFileName.class);
    private ScmSession ss;
    private String wsName = TestBatchUniqueFileName.class.getSimpleName();

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
        conf.setBatchFileNameUnique(true);

        ScmWorkspace ws = ScmFactory.Workspace.createWorkspace(ss, conf);
        ScmRole role = ScmFactory.Role.getRole(ss, "ROLE_AUTH_ADMIN");
        ScmFactory.Role.grantPrivilege(ss, role, ScmResourceFactory.createWorkspaceResource(wsName),
                ScmPrivilegeType.ALL);
        Thread.sleep(20000);

        ScmBatch batch = ScmFactory.Batch.createInstance(ws);
        batch.setName("test");
        batch.save();

        ScmDirectory d = ScmFactory.Directory.createInstance(ws, "/a");

        ScmFile file1 = ScmFactory.File.createInstance(ws);
        file1.setFileName("file1");
        file1.setDirectory(d);
        file1.save();

        ScmFile file2 = ScmFactory.File.createInstance(ws);
        file2.setFileName("file1");
        file2.save();

        ScmFile file3 = ScmFactory.File.createInstance(ws);
        file3.setFileName("file3");
        file3.save();

        batch.attachFile(file1.getFileId());
        batch.attachFile(file3.getFileId());
        try {
            batch.attachFile(file2.getFileId());
            Assert.fail();
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.BATCH_FILE_SAME_NAME) {
                throw e;
            }
        }

        file2.delete(true);
        try {
            file3.setFileName("file1");
            Assert.fail();
        }
        catch (ScmException e) {
            if (e.getError() != ScmError.BATCH_FILE_SAME_NAME) {
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
