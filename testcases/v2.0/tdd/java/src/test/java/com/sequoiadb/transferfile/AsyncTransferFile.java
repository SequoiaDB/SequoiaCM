package com.sequoiadb.transferfile;

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
 * 通过分中心2执行文件迁移，将分中心2的文件迁移到主中心1
 * 文件           中心id
 * file1   2
 *
 * 从分中心2执行文件迁移 预期结果
 * 文件           中心id
 * file1   2 1
 *
 * @author linyoubin
 *
 */
public class AsyncTransferFile extends ScmTestMultiCenterBase {
    private static final Logger logger = Logger.getLogger(AsyncTransferFile.class);

    private String workingDir;
    private ScmSession site2Session;
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

        //create file from site2
        site2Session = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), site2Session);
        srcFile = fileDir + File.separator + "file1.txt";
        ScmTestTools.createFile(srcFile, ScmTestTools.generateString(10), 1025 * 1024);
        fileId = ScmTestTools.createScmFile(ws, srcFile, AsyncTransferFile.class.getName(), "", "").getFileId();
    }

    @Test
    public void transferFile() throws ScmException, InterruptedException, IOException {

        long tid = Thread.currentThread().getId();
        String outputDir = workingDir + File.separator + ScmTestTools.getMethodName()
        + File.separator + tid;
        ScmTestTools.createDir(outputDir);
        String downFile = outputDir + File.separator + "down.txt";

        List<ScmFileLocation> locationList;
        ScmFile file = ScmFactory.File.getInstance(ws, fileId);
        locationList = file.getLocationList();
        //before cache, only exists in site2
        Assert.assertFalse(ScmTestTools.isSiteExist(locationList, getSiteId1()),
                ScmTestTools.formatLocationList(locationList));
        Assert.assertTrue(ScmTestTools.isSiteExist(locationList, getSiteId2()),
                ScmTestTools.formatLocationList(locationList));
        Assert.assertFalse(ScmTestTools.isSiteExist(locationList, getSiteId3()),
                ScmTestTools.formatLocationList(locationList));

        // cache file
        ScmFactory.File.asyncTransfer(ws, fileId);
        while (true) {
            file = ScmFactory.File.getInstance(ws, fileId);
            locationList = file.getLocationList();
            if (ScmTestTools.isSiteExist(locationList, getSiteId1())) {
                break;
            }

            Thread.sleep(100);
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
        site2Session.close();
    }

}

