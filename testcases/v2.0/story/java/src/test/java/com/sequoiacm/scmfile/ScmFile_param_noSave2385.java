package com.sequoiacm.scmfile;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @Testcase: SCM-2385:创建文件实例，对未save的实例进行toString
 * @author fanyu init
 * @date 2019.02.16
 */

public class ScmFile_param_noSave2385 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String fileName = "file2385";
    private WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession(site);
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(wsp.getName(), session);
        ScmFile file = ScmFactory.File.createInstance(ws);
        file.setFileName(fileName);
        Assert.assertTrue(file.toString().contains(fileName),file.toString());
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        session.close();
    }
}
