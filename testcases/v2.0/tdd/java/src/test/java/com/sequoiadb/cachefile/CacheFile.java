package com.sequoiadb.cachefile;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
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
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/**
 * 通过中心2执行文件缓存，将主中心1的文件cache到中心2
 * 文件           中心id
 * file1   1
 *
 * 从中心1执行文件缓存 预期结果
 * 文件           中心id
 * file1   2 1
 *
 * @author linyoubin
 *
 */
public class CacheFile extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(CacheFile.class);

    private String workingDir;
    private ScmSession site1Session;
    private ScmWorkspace ws;

    private String srcFile;
    private ScmId fileId;

    @BeforeClass
    public void setUp() throws ScmException {
        workingDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.deleteDir(workingDir);
        ScmTestTools.createDir(workingDir);

        long createFileTid = Thread.currentThread().getId();
        String fileDir = workingDir + File.separator + createFileTid;
        ScmTestTools.createDir(fileDir);

        site1Session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer1().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), site1Session);
        srcFile = fileDir + File.separator + "file1.txt";
        ScmTestTools.createFile(srcFile, ScmTestTools.generateString(10), 1025 * 1024);
        fileId = ScmTestTools.createScmFile(ws, srcFile, CacheFile.class.getName(), "", "").getFileId();
    }

    @Test
    public void cacheFile() throws ScmException, InterruptedException, IOException {

        long tid = Thread.currentThread().getId();
        String outputDir = workingDir + File.separator + ScmTestTools.getMethodName()
        + File.separator + tid;
        ScmTestTools.createDir(outputDir);
        String downFile = outputDir + File.separator + "down.txt";

        ScmSession site2Session = null;
        List<ScmFileLocation> locationList;
        try {
            site2Session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                    getServer2().getUrl(), getScmUser(), getScmPasswd()));
            ScmWorkspace ws2 = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), site2Session);

            ScmFile file = ScmFactory.File.getInstance(ws2, fileId);
            locationList = file.getLocationList();
            //before cache, only exists in site1
            Assert.assertTrue(ScmTestTools.isSiteExist(locationList, getSiteId1()),
                    ScmTestTools.formatLocationList(locationList));
            Assert.assertFalse(ScmTestTools.isSiteExist(locationList, getSiteId2()),
                    ScmTestTools.formatLocationList(locationList));
            Assert.assertFalse(ScmTestTools.isSiteExist(locationList, getSiteId3()),
                    ScmTestTools.formatLocationList(locationList));

            // cache file
            ScmFactory.File.asyncCache(ws2, fileId);
            while (true) {
                file = ScmFactory.File.getInstance(ws2, fileId);
                locationList = file.getLocationList();
                if (ScmTestTools.isSiteExist(locationList, getSiteId2())) {
                    break;
                }

                Thread.sleep(100);
            }
        }
        finally {
            if (null != site2Session) {
                site2Session.close();
            }
        }

        logger.info(ScmTestTools.formatLocationList(locationList));
        //after cache, exists in site1 and site2
        Assert.assertTrue(ScmTestTools.isSiteExist(locationList, getSiteId1()),
                ScmTestTools.formatLocationList(locationList));
        Assert.assertTrue(ScmTestTools.isSiteExist(locationList, getSiteId2()),
                ScmTestTools.formatLocationList(locationList));
        Assert.assertFalse(ScmTestTools.isSiteExist(locationList, getSiteId3()),
                ScmTestTools.formatLocationList(locationList));

        // check site2
        ScmTestTools.checkLob(getServer2().getUrl(), getScmUser(),
                getScmPasswd(), getWorkspaceName(), fileId, srcFile, downFile);

        // check mainsite
        ScmTestTools.checkLob(getServer1().getUrl(), getScmUser(),
                getScmPasswd(), getWorkspaceName(), fileId, srcFile, downFile);

        try {
            // check site3
            ScmTestTools.checkLob(getServer3().getUrl(), getScmUser(),
                    getScmPasswd(), getWorkspaceName(), fileId, srcFile, downFile);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getErrorCode(),
                    ScmError.DATA_NOT_EXIST.getErrorCode(), e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmTestTools.removeScmFileSilence(ws, fileId);
        site1Session.close();
    }
}

