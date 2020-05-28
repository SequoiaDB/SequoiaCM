package com.sequoiacm.net.readcachefile.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.SkipException;
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
import com.sequoiadb.exception.BaseException;

/**
 * @Description:SCM-732 :并发跨中心读取多个ws下的文件 1、分中心A，在多个ws下写入文件； 2、在主中心并发读取分中心A写入的文件；
 *                      3、检查文件元数据和内容正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class MainCenterReadMutiWsFile732 extends TestScmBase {
    private static final String author = "MainCenterReadMutiWsFile732";
    private final int wsNum = 3;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private List< WsWrapper > wsList = null;
    private int fileSize = 1024 * 200;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
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
            if ( ScmInfo.getWsNum() < wsNum ) {
                throw new SkipException( "Skip!" );
            }

            rootSite = ScmInfo.getRootSite();
            branSite = ScmInfo.getBranchSite();
            wsList = ScmInfo.getWss( wsNum );

            prepareFiles( branSite, wsList.get( 0 ) );
            prepareFiles( branSite, wsList.get( 1 ) );
            prepareFiles( branSite, wsList.get( 2 ) );
        } catch ( IOException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        ReadScmFile rThread1 = new ReadScmFile( rootSite, wsList.get( 0 ),
                fileIdList.get( 0 ) );
        rThread1.start( 10 );

        ReadScmFile rThread2 = new ReadScmFile( rootSite, wsList.get( 1 ),
                fileIdList.get( 1 ) );
        rThread2.start( 10 );

        ReadScmFile rThread3 = new ReadScmFile( rootSite, wsList.get( 2 ),
                fileIdList.get( 2 ) );
        rThread3.start( 10 );

        if ( !( rThread1.isSuccess() && rThread2.isSuccess()
                && rThread3.isSuccess() ) ) {
            Assert.fail( rThread1.getErrorMsg() + rThread2.getErrorMsg()
                    + rThread3.getErrorMsg() );
        }
        checkResult( fileIdList.get( 0 ), wsList.get( 0 ) );
        checkResult( fileIdList.get( 1 ), wsList.get( 1 ) );
        checkResult( fileIdList.get( 2 ), wsList.get( 2 ) );
        checkMetadataAndLobs();

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();
                ScmFileUtils.cleanFile( wsList.get( 0 ), cond );
                ScmFileUtils.cleanFile( wsList.get( 1 ), cond );
                ScmFileUtils.cleanFile( wsList.get( 2 ), cond );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {

        }
    }

    private void checkResult( ScmId fileId, WsWrapper wsp )
            throws ScmException {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );

            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            Assert.assertEquals( file.getWorkspaceName(), ws.getName() );
            Assert.assertEquals( file.getFileId(), fileId );
            Assert.assertEquals( file.getAuthor(), author );
            Assert.assertEquals( file.getSize(), fileSize );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertNotNull( file.getCreateTime().getTime() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private void checkMetadataAndLobs() throws Exception {
        // check meta data
        SiteWrapper[] expSites = { rootSite, branSite };
        ScmFileUtils.checkMetaAndData( wsList.get( 0 ),
                fileIdList.subList( 0, 1 ), expSites, localPath, filePath );
        ScmFileUtils.checkMetaAndData( wsList.get( 1 ),
                fileIdList.subList( 1, 2 ), expSites, localPath, filePath );
        ScmFileUtils.checkMetaAndData( wsList.get( 2 ),
                fileIdList.subList( 2, 3 ), expSites, localPath, filePath );
    }

    private void prepareFiles( SiteWrapper site, WsWrapper wsp ) {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setContent( filePath );
            scmfile.setFileName( author + "_" + UUID.randomUUID() );
            scmfile.setAuthor( author );
            fileIdList.add( scmfile.save() );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != session ) {
                session.close();
            }
        }
    }

    private class ReadScmFile extends TestThreadBase {
        private SiteWrapper site = null;
        private WsWrapper wsp = null;
        private ScmId fileId = null;

        public ReadScmFile( SiteWrapper site, WsWrapper wsp, ScmId fileId ) {
            this.site = site;
            this.wsp = wsp;
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
                if ( null != session ) {
                    session.close();
                }
            }
        }
    }
}
