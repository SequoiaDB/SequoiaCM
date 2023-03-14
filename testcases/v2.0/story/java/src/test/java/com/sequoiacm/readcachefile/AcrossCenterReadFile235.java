package com.sequoiacm.readcachefile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @Testcase: SCM-235:A中心写文件，B中心读取文件后再在主中心读取文件（A/B网络不通） 1、分中心A写文件 2、分中心B读取文件
 *            3、删除分中心A、分中心B的LOB文件，在主中心读取文件（测试主中心是否读的本地缓存）
 * @author huangxiaoni init
 * @date 2017.5.4
 */

public class AcrossCenterReadFile235 extends TestScmBase {
    private final int branSitesNum = 3;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmSession ss = null;
    private ScmWorkspace work = null;

    private String fileName = "readCacheFile235";
    private ScmId fileId = null;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
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

            ss = ScmSessionUtils.createSession( branSites.get( 0 ) );
            work = ScmFactory.Workspace.getWorkspace( wsp.getName(), ss );
        } catch ( BaseException | IOException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite", "star" }, enabled = false)
    private void test() throws Exception {
        this.writeFileFromA();
        this.readFileFromB();

        TestSdbTools.Lob.removeLob( branSites.get( 0 ), wsp, fileId );
        TestSdbTools.Lob.removeLob( branSites.get( 1 ), wsp, fileId );

        this.readFileFromM();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( work, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( ss != null ) {
                ss.close();
            }

        }
    }

    private void writeFileFromA() throws Exception {
        try {
            // write scmfile
            fileId = ScmFileUtils.create( work,
                    fileName + "_" + UUID.randomUUID(), filePath );

            // check results
            SiteWrapper[] expSites = { branSites.get( 0 ) };
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                    filePath );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void readFileFromB() throws Exception {
        ScmSession session = null;
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            session = ScmSessionUtils.createSession( branSites.get( 1 ) );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );

            // read scmfile
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
            // checkFreeSite(branSites.get(2));
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
            if ( session != null )
                session.close();
        }
    }

    private void readFileFromM() throws Exception {
        ScmSession ss = null;
        ScmInputStream sis = null;
        OutputStream fos = null;
        try {
            // login
            ss = ScmSessionUtils.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    ss );

            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            fos = new FileOutputStream( new File( downloadPath ) );
            sis = ScmFactory.File.createInputStream( scmfile );
            sis.read( fos );

            // check content
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );

            // check meta data
            SiteWrapper[] expSites = { rootSite, branSites.get( 0 ),
                    branSites.get( 1 ) };
            ScmFileUtils.checkMeta( ws, fileId, expSites );
            checkFreeSite( branSites.get( 2 ) );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
            if ( ss != null )
                ss.close();
        }
    }

    private void checkFreeSite( SiteWrapper site ) throws Exception {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            ScmFileUtils.checkData( ws, fileId, localPath, filePath );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DATA_NOT_EXIST
                    && e.getError() != ScmError.DATA_ERROR ) {// TODO:jira:158
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}