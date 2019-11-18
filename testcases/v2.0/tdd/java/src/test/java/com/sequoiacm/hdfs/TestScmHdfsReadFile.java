package com.sequoiacm.hdfs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;
import com.sequoiacm.testcommon.ScmTestTools;

public class TestScmHdfsReadFile extends ScmTestMultiCenterBase {

    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmFile file;
    private ScmId scmId = null;
    private ScmInputStream is = null;
    private OutputStream os = null;
    private String srcFile;
    private String downFile;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer3().getUrl(), getScmUser(), getScmPasswd()));
        
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);
        file = ScmFactory.File.createInstance(ws);
        srcFile = getDataDirectory() + File.separator + "test.txt";
        downFile = getDataDirectory() + File.separator + "down.txt";
        file.setContent(srcFile);
        file.setFileName("test_hdfs_read_file");
        scmId = file.save();
    }

    @Test
    public void testRead() throws ScmException, IOException {
        
        
        ScmFile read_File = ScmFactory.File.getInstance(ws, scmId);
        is = ScmFactory.File.createInputStream(read_File);
        os = new FileOutputStream(downFile);
        is.read(os);
        os.close();
        is.close();
        is = null;
        os = null;
        Assert.assertEquals(ScmTestTools.getMD5(srcFile), ScmTestTools.getMD5(downFile));

    }

    @AfterClass
    public void tearDown() throws ScmException {
        file.delete(true);
        ss.close();
    }

}
