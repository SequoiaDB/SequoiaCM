package com.sequoiacm.scmfile;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
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
import com.sequoiacm.common.ScmUpdateContentOption;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description: SCM-2973 :: 计算已有文件md5,文件有多版本，部分版本无md5且在远程
 * @author fanyu
 * @Date:2020年8月27日
 * @version:1.0
 */
public class ScmFileMD5Calc2973 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper site1 = null;
    private SiteWrapper site2 = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String fileName = "file2973_" + UUID.randomUUID();
    private ScmId fileId;
    private int fileSize = 1024 * 200;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        List< SiteWrapper > sites = ScmInfo.getAllSites();
        site1 = sites.get( 0 );
        site2 = sites.get( 1 );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site1 );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        // 创建无md5的文件
        byte[] bytes = new byte[ fileSize ];
        new Random().nextBytes( bytes );
        createFile( fileName, bytes );

        // 更新文件内容，使文件有多版本
        int versionNum = 5;
        for ( int i = 2; i <= versionNum; i++ ) {
            if ( i % 2 == 0 ) {
                updateFile( bytes, true );
            } else {
                updateFile( bytes, false );
            }
        }

        // 连接站点B,指定版本计算文件md5
        ScmSession sessionB = null;
        try {
            sessionB = TestScmTools.createSession( site2 );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    sessionB );
            for ( int i = 1; i <= versionNum; i++ ) {
                ScmFile file = ScmFactory.File.getInstance( ws, fileId, i, 0 );
                file.calcMd5();
            }
        } finally {
            if ( sessionB != null ) {
                sessionB.close();
            }
        }

        // 检查文件md5属性
        String expMd5 = TestTools.getMD5AsBase64( new ByteArrayInputStream( bytes ) );
        for ( int i = 1; i <= versionNum; i++ ) {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId, i, 0 );
            Assert.assertEquals( file.getMd5(), expMd5, fileId.get() );
        }
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

    private void updateFile( byte[] bytes, boolean isNeedMd5 )
            throws ScmException, FileNotFoundException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        ScmUpdateContentOption option = new ScmUpdateContentOption();
        option.setNeedMd5( isNeedMd5 );
        file.updateContent( new ByteArrayInputStream( bytes ), option );
    }
}