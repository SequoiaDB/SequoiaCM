package com.sequoiacm.file;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * 1、write file in site2
 * 2、read file from site3
 * result: both site1(main site) and site3 no cache local
 */
public class ReadFileForceNoCache extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(ReadFileForceNoCache.class);

    private String workingDir = null;
    private String srcFile = null;
    private int fileSize = 1024 * 1024 * 2 + 1;

    @BeforeClass
    public void setUp() {
        logger.info("test begin:" + ScmTestTools.getClassName());
        workingDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        srcFile = workingDir + File.separator + "src.txt";

        ScmTestTools.deleteDir(workingDir);
        // ready file
        long time = System.currentTimeMillis();
        String value = Long.toString(time);
        ScmTestTools.createFile(srcFile, value, fileSize);
    }

    @Test
    public void testReadFileForceNoCache() throws ScmException {

        ScmSession ss = null;
        ScmFile scmFile = null;
        ScmId fileId = null;
        String wsName = getWorkspaceName();
        try {
            // write file on site2
            ss = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(getServer2().getUrl(),
                            getScmUser(), getScmPasswd()));
            ScmWorkspace ws1 = ScmFactory.Workspace.getWorkspace(wsName, ss);
            scmFile = ScmTestTools.createScmFile(ws1, srcFile, ScmTestTools.getClassName(),
                    "author", "testFNC");
            fileId = scmFile.getFileId();

            // check file's location site is site2
            List<ScmFileLocation> siteList = scmFile.getLocationList();
            System.out.println(siteList.toString());
            Assert.assertFalse(CommonHelper.isSiteExist(getSiteId1(), siteList),
                    ScmTestTools.formatLocationList(siteList));
            Assert.assertTrue(CommonHelper.isSiteExist(getSiteId2(), siteList),
                    ScmTestTools.formatLocationList(siteList));
            Assert.assertFalse(CommonHelper.isSiteExist(getSiteId3(), siteList),
                    ScmTestTools.formatLocationList(siteList));
            ss.close();

            // read file from site3
            ss = ScmFactory.Session.createSession(ScmType.SessionType.AUTH_SESSION,
                    new ScmConfigOption(getServer3().getUrl(),
                            getScmUser(), getScmPasswd()));
            ScmWorkspace ws2 = ScmFactory.Workspace.getWorkspace(wsName, ss);
            String destDir = workingDir + File.separator + ScmTestTools.getMethodName();
            String destFile = destDir + File.separator + "out1.txt";
            ScmTestTools.createDir(destDir);
            ScmTestTools.readAndCheckFile(ws2, fileId, srcFile, destFile,
                    CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE);

            scmFile = ScmFactory.File.getInstance(ws2, fileId);

            // check file's location site is site2
            siteList = scmFile.getLocationList();
            Assert.assertFalse(CommonHelper.isSiteExist(getSiteId1(), siteList),
                    ScmTestTools.formatLocationList(siteList));
            Assert.assertTrue(CommonHelper.isSiteExist(getSiteId2(), siteList),
                    ScmTestTools.formatLocationList(siteList));
            Assert.assertFalse(CommonHelper.isSiteExist(getSiteId3(), siteList),
                    ScmTestTools.formatLocationList(siteList));

            // check site1 and site3 have no lob
            Assert.assertFalse(ScmTestTools.isLocalSiteDataExist(getServer1().getUrl(), getScmUser(),
                    getScmPasswd(), wsName, scmFile.getFileId()));
            Assert.assertFalse(ScmTestTools.isLocalSiteDataExist(getServer3().getUrl(), getScmUser(),
                    getScmPasswd(), wsName, scmFile.getFileId()));

            ScmTestTools.deleteScmFile(getServer1().getUrl(), getScmUser(),
                    getScmPasswd(), getWorkspaceName(), fileId.get());
        }
        catch (Exception e) {
            logger.error("testReadFileForceNoCache failed", e);
            Assert.fail(e.getMessage());
        }
        finally {
            ScmTestTools.releaseSession(ss);
        }
    }
}
