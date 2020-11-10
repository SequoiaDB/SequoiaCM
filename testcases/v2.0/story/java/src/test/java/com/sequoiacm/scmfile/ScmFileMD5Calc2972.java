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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2972:计算已有文件md5,文件在本地且无md5
 * @author fanyu
 * @Date:2020年8月27日
 * @version:1.0
 */
public class ScmFileMD5Calc2972 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file2972_" + UUID.randomUUID();
    private ScmId fileId;
    private int fileSize = 1024 * 200;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // 创建无md5的文件
        byte[] bytes = new byte[ fileSize ];
        new Random().nextBytes( bytes );
        createFile( fileName, bytes );

        // 计算已有文件md5
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.calcMd5();

        // 检查文件md5属性
        Assert.assertEquals( file.getMd5(),
                TestTools.getMD5AsBase64( new ByteArrayInputStream( bytes ) ) );
        runSuccess = true;
    }
    
    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
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