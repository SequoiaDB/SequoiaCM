package com.sequoiacm.physicaldelete;

import java.io.File;

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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;
/**
 * 物理删除一个文件，查看删除结果
 * @author huangqiaohui
 *
 */
public class TestPhysicalDelete extends ScmTestMultiCenterBase {
    private String srcFile;
    private ScmId fileID;
    private ScmSession ss;
    private ScmWorkspace ws;
    private String metaCS;
    private String LobCS;

    @BeforeClass
    public void setUp() throws ScmException {
        metaCS = getWorkspaceName() + "_META";
        LobCS = getWorkspaceName() + "_LOB";
        srcFile = getDataDirectory() + File.separator + "test.txt";
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer2().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        fileID = ScmTestTools.createScmFile(ws, srcFile, TestPhysicalDelete.class.getName(), "", "").getFileId();
    }

    @Test
    public void physicalDelete() throws ScmException {

        // delete file
        ScmFile file = ScmFactory.File.getInstance(ws, fileID);
        file.delete(true);

        // get file again
        try {
            ScmFile file2 = ScmFactory.File.getInstance(ws, fileID);
            Assert.fail("getInstance success:" + file2);
        }
        catch (ScmException e) {
            Assert.assertEquals(e.getErrorCode(), ScmError.FILE_NOT_FOUND.getErrorCode(),
                    e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ss.close();
    }
}
