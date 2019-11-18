package com.sequoiacm.scmfile;

import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Testcase: SCM-171:getInstance有效参数校验
 * @author huangxiaoni init
 * @date 2017.4.12
 */

public class ScmFile_param_getInstance_valid171 extends TestScmBase {
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "scmfile171";
    private ScmId fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession(site);
            ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);

            fileId = createScmFile(ws);
        } catch (ScmException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testMajorVersionNotExist() {
        try {
            ScmFile file = ScmFactory.File.getInstance(ws, fileId, 2, 0); // "2"
            // not
            // exist
            Assert.assertNull(file);
            file.getFileName();
            Assert.assertFalse(true, "expect result is fail but actual is success.");
        } catch (ScmException e) {
            if (e.getError() !=  ScmError.FILE_NOT_FOUND ) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testMinorVersionNotExist() {
        try {
            ScmFile file = ScmFactory.File.getInstance(ws, fileId, 1, 3); // "3"
            // not
            // exist
            Assert.assertNull(file);
            file.getFileName();
            Assert.assertFalse(true, "expect result is fail but actual is success.");
        } catch (ScmException e) {
            if (e.getError() !=  ScmError.FILE_NOT_FOUND) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        }
        runSuccess2 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ((runSuccess1 && runSuccess2) || TestScmBase.forceClear) {
                ScmFactory.File.deleteInstance(ws, fileId, true);
            }
        } catch (ScmException e) {
            Assert.fail(e.getMessage());
        } finally {
            session.close();

        }
    }

    private ScmId createScmFile(ScmWorkspace ws) {
        ScmId scmFileID = null;
        try {
            ScmFile file = ScmFactory.File.createInstance(ws);
            file.setFileName(fileName+"_"+UUID.randomUUID());
            file.setTitle("sequoiacm");
            file.setMimeType("text/plain");
            scmFileID = file.save();
        } catch (ScmException e) {
            Assert.fail(e.getMessage());
        }
        return scmFileID;
    }

}