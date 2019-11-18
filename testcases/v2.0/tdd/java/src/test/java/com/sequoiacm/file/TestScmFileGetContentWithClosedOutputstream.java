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
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonHelper;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/**
 * B中心通过Scmfile的getContent获取文件意外中断时(通过传入一个关闭的输出流实现)，查看各中心的Lob状态
 *
 * @author huangqiaohui
 *
 */
public class TestScmFileGetContentWithClosedOutputstream extends ScmTestMultiCenterBase {

    private String srcFile;
    private String downFile;
    private ScmFile bScmFile;
    private ScmSession bSiteSs;

    @BeforeClass
    public void setUp() throws ScmException {
        srcFile = getDataDirectory() + File.separator + "test.txt";
        downFile = getDataDirectory() + File.separator + "down.txt";

        // site A createFile
        bSiteSs = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), bSiteSs);
        bScmFile = ScmTestTools.createScmFile(ws, srcFile, ScmTestTools.getClassName(), "",
                "testTitle");
    }

    @Test
    public void getFile() throws ScmException, IOException {

        // create ScmFileInputStream
        ScmSession ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION,
                new ScmConfigOption(getServer3().getUrl(), getScmUser(),
                        getScmPasswd()));

        try {
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
            ScmFile sf = ScmFactory.File.getInstance(ws, bScmFile.getFileId());
            OutputStream os = new FileOutputStream(downFile);

            //close outputStream
            os.close();
            try {
                sf.getContent(os); //getContent with a closed outputStream
                Assert.fail("test fail,getContent success ");
            }
            catch (ScmException e) {
                Assert.assertEquals(e.getErrorCode(), -601, e.getMessage());
            }

            ScmFile tempFile = ScmFactory.File.getInstance(ws, bScmFile.getFileId());
            // check file's location site is site1 & site2 site3
            List<ScmFileLocation> siteList = tempFile.getLocationList();

            Assert.assertTrue(CommonHelper.isSiteExist(getSiteId2(), siteList),
                    ScmTestTools.formatLocationList(siteList));
            if(CommonHelper.isSiteExist(getSiteId3(), siteList)) {
                Assert.assertTrue(CommonHelper.isSiteExist(getSiteId1(), siteList),
                        ScmTestTools.formatLocationList(siteList));
            }
            
            // check site2
            ScmTestTools.checkLob(getServer2().getUrl(), getScmUser(),
                    getScmPasswd(), getWorkspaceName(), tempFile.getFileId(), srcFile, downFile);
        }
        finally {
            if (null != ss) {
                ss.close();
            }
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        bScmFile.delete(true);
        bSiteSs.close();
    }
}
