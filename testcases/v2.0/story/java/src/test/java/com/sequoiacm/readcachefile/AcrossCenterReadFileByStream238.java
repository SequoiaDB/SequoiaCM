package com.sequoiacm.readcachefile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-238:A中心写文件，B中心调用read输出流方式分批读取文件 1、分中心A写文件，文件大小>1M 2、分中心B读取文件
 * @author huangxiaoni init
 * @date 2017.5.4
 */

public class AcrossCenterReadFileByStream238 extends TestScmBase {
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "readCacheFile238";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 3;
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

            rootSite = ScmInfo.getRootSite();
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsp = ScmInfo.getWs();

            session = TestScmTools.createSession( branSites.get( 0 ) );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        try {
            // writeFileFromA
            fileId = ScmFileUtils.create( ws, fileName, filePath );
            this.readFileFromB();
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.getInstance( ws, fileId ).delete( true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void readFileFromB() throws Exception {
        ScmSession session = null;
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            // login
            session = TestScmTools.createSession( branSites.get( 1 ) );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );

            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );

            fos = new FileOutputStream( new File( downloadPath ) );
            sis = ScmFactory.File.createInputStream( scmfile );
            sis.read( fos );

            // check results
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );

            SiteWrapper[] expSites = { rootSite, branSites.get( 0 ),
                    branSites.get( 1 ) };
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                    filePath );
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
            if ( session != null )
                session.close();
        }
    }

}