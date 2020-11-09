package com.sequoiacm.scmfile;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * @Description: SCM-2979:save参数校验
 * @author fanyu
 * @Date:2020年8月28日
 * @version:1.0
 */

public class ScmFile_param_save2979 extends TestScmBase {
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test1() throws ScmException, IOException {
        String fileName = "file2979A_" + UUID.randomUUID();
        ScmId fileId = null;
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setAuthor( fileName );
        file.setFileName( fileName );
        ScmUploadConf scmUploadConf = new ScmUploadConf( false );
        scmUploadConf.setNeedMd5( true );
        Assert.assertTrue( scmUploadConf.isNeedMd5() );
        fileId = file.save( scmUploadConf );
        Assert.assertEquals( file.getAuthor(), fileName, fileId.get() );
        Assert.assertEquals( file.getFileName(), fileName, fileId.get() );
        Assert.assertEquals( file.getMd5(), TestTools
                .getMD5AsBase64( new ByteArrayInputStream( new byte[ 0 ] ) ) );
        file.delete( true );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test2() throws ScmException {
        String fileName = "file2979B_" + UUID.randomUUID();
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        ScmId fileId = file.save( null );
        ScmFile file1 = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertNull( file1.getMd5() );
        file1.delete( true );
    }
    
    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }
}
