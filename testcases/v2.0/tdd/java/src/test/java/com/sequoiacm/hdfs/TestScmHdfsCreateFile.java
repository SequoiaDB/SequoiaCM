package com.sequoiacm.hdfs;

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
import com.sequoiacm.testcommon.ScmTestMultiCenterBase;

public class TestScmHdfsCreateFile extends ScmTestMultiCenterBase {

    private ScmSession ss;
    private ScmWorkspace ws;
    private ScmFile create_file;
    private ScmId scmId = null;

    @BeforeClass
    public void setUp() throws ScmException {
        ss = ScmFactory.Session.createSession(SessionType.AUTH_SESSION, new ScmConfigOption(
                getServer3().getUrl(), getScmUser(), getScmPasswd()));
        ws = ScmFactory.Workspace.getWorkspace(getWorkspaceName(), ss);

    }

    @Test
    public void testAttach() throws ScmException {

        create_file = ScmFactory.File.createInstance(ws);
        create_file.setContent(getDataDirectory() + File.separator + "test.txt");
        create_file.setFileName("test_hdfs_create_file");

        scmId = create_file.save();
        
        ScmFile read_File = ScmFactory.File.getInstance(ws, scmId);
        Assert.assertEquals(create_file.getFileName(), read_File.getFileName());

    }

    @AfterClass
    public void tearDown() throws ScmException {
        create_file.delete(true);
        ss.close();
    }

}
