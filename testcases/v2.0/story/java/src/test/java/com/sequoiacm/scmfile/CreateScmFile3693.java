package com.sequoiacm.scmfile;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Description SCM-3693:ScmFile对象save时，文件内容流关闭调用正确
 * @author YiPan
 * @Date 2021.7.21
 * @Version 1.00
 */
public class CreateScmFile3693 extends TestScmBase {
    private ScmSession session;
    private ScmWorkspace workspace;
    private String fileName = "file3693";
    private ScmId fileId;

    @BeforeClass
    public void setUp() throws ScmException {
        SiteWrapper rootSite = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( rootSite );
        workspace = ScmFactory.Workspace
                .getWorkspace( ScmInfo.getWs().getName(), session );
    }

    @Test
    public void test() throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( workspace );
        TestInputStream inputStream = new TestInputStream();
        file.setContent( inputStream );
        file.setFileName( fileName );
        fileId = file.save();
        Assert.assertEquals( inputStream.getCloseTimes(), 1 );
    }

    class TestInputStream extends InputStream {
        int closeTimes = 0;

        public int getCloseTimes() {
            return closeTimes;
        }

        @Override
        public int read() throws IOException {
            return -1;
        }

        @Override
        public void close() throws IOException {
            closeTimes++;
        }
    }

    @AfterClass
    public void tearDown() throws ScmException {
        ScmFactory.File.deleteInstance( workspace, fileId, true );
        session.close();
    }

}