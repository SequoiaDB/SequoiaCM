package com.sequoiacm.file;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/*
 * write file in main site(site1) read file from site2
 */

public class ReadFileFromOtherCenter extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(ReadFileFromOtherCenter.class);

    private String workingDir = null;
    private String srcFile = null;
    private int fileSize = 1024 * 1024 * 2 + 1;

    @BeforeClass
    public void setUp() {
        logger.info("test begin:" + ScmTestTools.getClassName());
        workingDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        srcFile = workingDir + File.separator + "src.txt";
        try {
            ScmTestTools.deleteDir(workingDir);
            // ready file
            long time = System.currentTimeMillis();
            String value = Long.toString(time);
            logger.info("value=" + value);
            ScmTestTools.createFile(srcFile, value, fileSize);
        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testReadFileFromOtherSite() throws ScmException {

        ScmSession ss = null;
        ScmFile scmFile = null;
        ScmId fileId = null;
        String wsName = getWorkspaceName();
        try {
            // wirte file on site1
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(getServer1().getUrl(),
                            getScmUser(), getScmPasswd()));
            ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace(wsName, ss);
            scmFile = ScmTestTools.createScmFile(ws1, srcFile, ScmTestTools.getClassName(),
                    "author", "t1");
            fileId = scmFile.getFileId();

            // check file's location site is site1
            List<ScmFileLocation> siteList = scmFile.getLocationList();
            System.out.println(siteList.toString());
            Assert.assertTrue(CommonHelper.isSiteExist(getSiteId1(), siteList),
                    ScmTestTools.formatLocationList(siteList));
            Assert.assertFalse(CommonHelper.isSiteExist(getSiteId2(), siteList),
                    ScmTestTools.formatLocationList(siteList));
            Assert.assertFalse(CommonHelper.isSiteExist(getSiteId3(), siteList),
                    ScmTestTools.formatLocationList(siteList));
            ss.close();

            // read file from site2
            ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                    new ScmConfigOption(getServer2().getUrl(),
                            getScmUser(), getScmPasswd()));
            ScmWorkspace ws2 = ScmFactory.Workspace.getWorkspace(wsName, ss);
            String destDir = workingDir + File.separator + ScmTestTools.getMethodName();
            String destFile = destDir + File.separator + "out1.txt";
            ScmTestTools.createDir(destDir);
            ScmTestTools.readAndCheckFile(ws2, fileId, srcFile, destFile);

            scmFile = ScmFactory.File.getInstance(ws2, fileId);

            // check file's location site is site1 & site2
            siteList = scmFile.getLocationList();
            Assert.assertTrue(CommonHelper.isSiteExist(getSiteId1(), siteList),
                    ScmTestTools.formatLocationList(siteList));
            Assert.assertTrue(CommonHelper.isSiteExist(getSiteId2(), siteList),
                    ScmTestTools.formatLocationList(siteList));
            Assert.assertFalse(CommonHelper.isSiteExist(getSiteId3(), siteList),
                    ScmTestTools.formatLocationList(siteList));

            // check site1's lob
            String checkLobFile1 = destDir + File.separator + "checklob1.txt";
            ScmTestTools.checkLob(getServer1().getUrl(), getScmUser(),
                    getScmPasswd(), wsName, scmFile.getFileId(), srcFile, checkLobFile1);

            // check site2's lob
            ScmTestTools.checkLob(getServer2().getUrl(), getScmUser(),
                    getScmPasswd(), wsName, scmFile.getFileId(), srcFile, checkLobFile1);

            ScmTestTools.deleteScmFile(getServer1().getUrl(), getScmUser(),
                    getScmPasswd(), getWorkspaceName(), fileId.get());
        }
        catch (Exception e) {
            logger.error("testReadFileFromOtherSite failed", e);
            Assert.fail(e.getMessage());
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}