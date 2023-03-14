package com.sequoiacm.scmfile;

import java.io.File;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Testcase: SCM-181:setContent参数校验
 * @author huangxiaoni init
 * @date 2017.4.12
 */

public class ScmFile_param_setContent181 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private boolean runSuccess1 = false;
    private boolean runSuccess2 = false;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile181";
    private int fileSize = 1;

    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = ScmSessionUtils.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testPathNotExist() {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            file.setTitle( "sequoiacm" );
            file.setContent( localPath + File.separator + "a" + File.separator
                    + "a.txt" );
            file.save();
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            e.printStackTrace();
            if ( e.getError() != ScmError.FILE_NOT_EXIST ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess1 = true;
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testFileNotExist() {
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            file.setTitle( "sequoiacm" );
            file.setContent( localPath + File.separator + "a.txt" );
            file.save();
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.FILE_NOT_EXIST ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
        runSuccess2 = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( ( runSuccess1 && runSuccess2 ) || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

}