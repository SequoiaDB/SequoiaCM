package com.sequoiacm.scmfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2974 :: 计算不存在的文件md5
 * @author fanyu
 * @Date:2020年8月27日
 * @version:1.0
 */
public class ScmFileMD5Calc2974 extends TestScmBase {
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file2974_" + UUID.randomUUID();
    private ScmId fileId;
    private int fileSize = 1024 * 100;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        // 创建无md5的文件
        byte[] bytes = new byte[ fileSize ];
        new Random().nextBytes( bytes );
        createFile( fileName, bytes );

        // 获取文件
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );

        // 删除文件
        file.delete( true );

        // 计算文件md5
        try {
            file.calcMd5();
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_FOUND ) {
                throw e;
            }
        }
    }
    

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        if ( session != null ) {
            session.close();
        }
    }

    private void createFile( String fileName, byte[] bytes )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        ScmUploadConf scmUploadConf = new ScmUploadConf( true, false );
        file.setFileName( fileName );
        file.setContent( new ByteArrayInputStream( bytes ) );
        fileId = file.save( scmUploadConf );
    }
    
}