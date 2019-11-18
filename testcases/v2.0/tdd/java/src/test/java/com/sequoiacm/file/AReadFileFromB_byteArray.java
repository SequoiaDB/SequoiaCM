package com.sequoiacm.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/**
 * A中心通过ScmInputStream read（byteArray） 方法获取B中心的文件
 *
 * @author huangqiaohui
 *
 */
public class AReadFileFromB_byteArray extends ScmTestMultiCenterBase {
    private String srcFile;
    private String downFile;
    private ScmFile bScmFile;
    private String sdb2;
    private String sdb3;
    private String centerCoord;
    private ScmSession bSiteSs;

    @BeforeClass
    public void setUp() throws ScmException {
        String workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.createDir(workDir);
        srcFile = getDataDirectory() + File.separator + "test.txt";
        downFile = workDir + File.separator + "down.txt";
        sdb2 = getServer2().getUrl();
        sdb3 = getServer3().getUrl();
        centerCoord = getServer1().getUrl();

        // site B createFile
        bSiteSs = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));

        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), bSiteSs);
        bScmFile = ScmTestTools.createScmFile(ws, srcFile, ScmTestTools.getClassName(), "", "testTitle");
    }

    @Test
    public void getFile() throws ScmException, IOException {
        // site A get File
        ScmSession ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer3().getUrl(), getScmUser(),
                        getScmPasswd()));

        OutputStream os = null;
        ScmInputStream is = null;
        ScmWorkspace ws = null;
        try {
            ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
            ScmFile sf = ScmFactory.File.getInstance(ws, bScmFile.getFileId());
            is = ScmFactory.File.createInputStream(sf);
            os = new FileOutputStream(downFile);
            byte[] buf = new byte[1024];
            while (true) {
                int readSize = is.read(buf, 0, 1024);
                if (readSize < 0) {
                    break;
                }
                os.write(buf, 0, readSize);
            }
            os.close();
            Assert.assertEquals(ScmTestTools.getMD5(srcFile), ScmTestTools.getMD5(downFile));

            ScmFile tempFile = ScmFactory.File.getInstance(ws, bScmFile.getFileId());
            // check file's location site is site1 & site2
            List<ScmFileLocation> siteList = tempFile.getLocationList();
            Assert.assertTrue(CommonHelper.isSiteExist(getSiteId1(), siteList),
                    ScmTestTools.formatLocationList(siteList));
            Assert.assertTrue(CommonHelper.isSiteExist(getSiteId2(), siteList),
                    ScmTestTools.formatLocationList(siteList));
            Assert.assertTrue(CommonHelper.isSiteExist(getSiteId3(), siteList),
                    ScmTestTools.formatLocationList(siteList));

            // check site2
            ScmTestTools.checkLob(getServer2().getUrl(), getScmUser(),
                    getScmPasswd(), getWorkspaceName(), tempFile.getFileId(), srcFile, downFile);

            // check site3
            ScmTestTools.checkLob(getServer3().getUrl(), getScmUser(),
                    getScmPasswd(), getWorkspaceName(), tempFile.getFileId(), srcFile, downFile);

            // check centerSite
            ScmTestTools.checkLob(getServer1().getUrl(), getScmUser(),
                    getScmPasswd(), getWorkspaceName(), tempFile.getFileId(), srcFile, downFile);
        }
        finally {
            if (os != null) {
                os.close();
            }
            if (is != null) {
                is.close();
            }
            ss.close();
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        bScmFile.delete(true);
        bSiteSs.close();
    }
}
