package com.sequoiacm.readcachefile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @Description: SCM-733 : 在分中心A的2个节点并发读取分中心B文件 1、在分中心B写入文件；
 *               2、在分中心A的2个不同节点并发读取文件； 3、检查读取结果正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class DiffNodeReadScmFile733 extends TestScmBase {
    private static final String author = "DiffNodeReadScmFile733";
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;
    private int fileNum = 1;
    private ScmId fileId = null;

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

            sessionA = TestScmTools.createSession( branSites.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
            prepareFiles( wsA );
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() {
        try {
            ReadScmFile rThread1 = new ReadScmFile( branSites.get( 1 ),
                    fileId );
            rThread1.start( 10 );

            ReadScmFile rThread2 = new ReadScmFile( branSites.get( 1 ),
                    fileId );
            rThread2.start( 10 );

            if ( !( rThread1.isSuccess() && rThread2.isSuccess() ) ) {
                Assert.fail( rThread1.getErrorMsg() + rThread2.getErrorMsg() );
            }

            checkResult();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();
                ScmFileUtils.cleanFile( wsp, cond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void prepareFiles( ScmWorkspace ws ) {
        ScmFile scmfile;
        try {
            for ( int i = 0; i < fileNum; ++i ) {
                scmfile = ScmFactory.File.createInstance( ws );
                scmfile.setContent( filePath );
                scmfile.setFileName( author + "_" + UUID.randomUUID() );
                scmfile.setAuthor( author );
                fileId = scmfile.save();
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void checkResult() {
        try {
            SiteWrapper[] expSites = { rootSite, branSites.get( 0 ),
                    branSites.get( 1 ) };
            ScmTaskUtils.waitAsyncTaskFinished( wsA, fileId, expSites.length );
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                    filePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private class ReadScmFile extends TestThreadBase {
        private SiteWrapper site = null;
        private ScmId fileId = null;

        public ReadScmFile( SiteWrapper site, ScmId fileId ) {
            this.site = site;
            this.fileId = fileId;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                file.getContent( downloadPath );
                Assert.assertEquals( TestTools.getMD5( filePath ),
                        TestTools.getMD5( downloadPath ) );
            } catch ( Exception e ) {
                Assert.fail( e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
