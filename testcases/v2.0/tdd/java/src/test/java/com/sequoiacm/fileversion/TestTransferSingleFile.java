package com.sequoiacm.fileversion;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestTransferSingleFile extends ScmTestMultiCenterBase {
    private ScmSession ssB;
    private ScmWorkspace wsB;
    private String file1Version1File;
    private String file1Version2File;
    private String downFile;
    private ScmSession ssM;
    private ScmWorkspace wsM;
    private ScmFile f1;
    private static final Logger logger = LoggerFactory.getLogger(TestTransferSingleFile.class);

    @BeforeClass
    public void init() throws ScmException {
        String workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.deleteDir(workDir);
        ScmTestTools.createDir(workDir);
        file1Version1File = workDir + File.separator + "file1version1.data";
        file1Version2File = workDir + File.separator + "file1version2.data";
        ScmTestTools.createFile(file1Version1File, "file1version1", 1024);
        ScmTestTools.createFile(file1Version2File, "file1version2", 2048);

        downFile = workDir + File.separator + "downFile.data";

        ssB = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer2().getUrl(), getScmUser(), getScmPasswd()));
        wsB = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ssB);

        ssM = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer1().getUrl(), getScmUser(), getScmPasswd()));
        wsM = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ssM);
        clearEnv();
    }

    private void clearEnv() throws ScmException {
        ScmCursor<ScmFileBasicInfo> c = ScmFactory.File.listInstance(wsB, ScopeType.SCOPE_CURRENT,
                ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR)
                .is(this.getClass().getSimpleName()).get());
        try {
            while (c.hasNext()) {
                ScmFactory.File.deleteInstance(wsB, c.getNext().getFileId(), true);
            }
        }
        finally {
            c.close();
        }
    }

    @Test
    public void test() throws ScmException, InterruptedException, IOException {
        f1 = ScmFactory.File.createInstance(wsB);
        f1.setFileName(this.getClass().getSimpleName() + "1");
        f1.setAuthor(this.getClass().getSimpleName());
        f1.setContent(file1Version1File);
        f1.save();
        f1.updateContent(file1Version2File);

        ScmFactory.File.asyncTransfer(wsB, f1.getFileId(), 1, 0);
        ScmFile f1InMain;
        long timeStamp = System.currentTimeMillis();
        while (true) {
            f1InMain = ScmFactory.File.getInstance(wsM, f1.getFileId(), 1, 0);
            if (f1InMain.getLocationList().size() != 1) {
                break;
            }
            if (System.currentTimeMillis() - timeStamp > 60 * 1000 * 5) {
                logger.info("wait for task finish");
            }
            Thread.sleep(500);
        }
        Assert.assertEquals(f1InMain.getLocationList().size(), 2, f1InMain.toString());
        Assert.assertNotEquals(f1InMain.getLocationList().get(0).getSiteId(),
                f1InMain.getLocationList().get(1).getSiteId());
        Assert.assertEquals(true,
                f1InMain.getLocationList().get(0).getSiteId() == getSiteId1()
                || f1InMain.getLocationList().get(0).getSiteId() == getSiteId2(),
                f1InMain.toString() + ", siteList=" + f1InMain.getLocationList());

        Assert.assertEquals(true,
                f1InMain.getLocationList().get(1).getSiteId() == getSiteId1()
                || f1InMain.getLocationList().get(1).getSiteId() == getSiteId2(),
                f1InMain.toString() + ", siteList=" + f1InMain.getLocationList());
        ScmTestTools.deleteFile(downFile);
        f1InMain.getContentFromLocalSite(downFile);
        Assert.assertEquals(ScmTestTools.getMD5(downFile), ScmTestTools.getMD5(file1Version1File));

        ScmFile f1Version2 = ScmFactory.File.getInstance(wsM, f1.getFileId());
        Assert.assertEquals(f1Version2.getLocationList().size(), 1, f1Version2.toString());
        Assert.assertEquals(f1Version2.getLocationList().get(0).getSiteId(), getSiteId2(), f1Version2.toString());
        ScmTestTools.deleteFile(downFile);
        try {
            f1Version2.getContentFromLocalSite(downFile);
            Assert.fail("read file in main site success:" + file1Version2File);
        }
        catch (Exception e) {

        }

        f1.delete(true);
    }

    @AfterClass
    public void tearDown() {
        ssB.close();
    }
}
