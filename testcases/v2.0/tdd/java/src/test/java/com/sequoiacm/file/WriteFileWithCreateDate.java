package com.sequoiacm.file;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.junit.Assert;
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
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

/**
 * 创建文件时带创建时间
 *
 */
public class WriteFileWithCreateDate extends ScmTestMultiCenterBase {

    private String srcFile;
    private ScmSession bSiteSs;
    private ScmWorkspace ws;
    private ScmId id;

    @BeforeClass
    public void setUp() throws ScmException {
        String workDir = getDataDirectory() + File.separator + ScmTestTools.getClassName();
        ScmTestTools.createDir(workDir);
        srcFile = getDataDirectory() + File.separator + "test.txt";
        // site A createFile
        bSiteSs = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));
    }

    @Test
    public void getFile() throws ScmException, IOException, ParseException {
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), bSiteSs);
        ScmFile file = ScmFactory.File.createInstance(ws);
        file.setTitle(ScmTestTools.getClassName());
        file.setContent(srcFile);
        file.setFileName(ScmTestTools.getClassName());
        Date d = new Date(1000 * 1000);
        file.setCreateTime(d);
        file.save();
        id = file.getFileId();

        Date d2 = file.getCreateTime();
        Assert.assertTrue("d=" + d + ",d2=" + d2, d.equals(d2));

        String dataId = file.getDataId().get();
        String fileId = id.get();
        Assert.assertTrue("data=" + dataId + ",fileId=" + fileId, dataId.equals(fileId));

        Assert.assertTrue(
                "fileCreateTime=" + file.getCreateTime() + ",dataCreateTime="
                        + file.getDataCreateTime(),
                file.getDataCreateTime().equals(file.getCreateTime()));
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmTestTools.removeScmFileSilence(ws, id);
        if (null != bSiteSs) {
            bSiteSs.close();
        }
    }
}
