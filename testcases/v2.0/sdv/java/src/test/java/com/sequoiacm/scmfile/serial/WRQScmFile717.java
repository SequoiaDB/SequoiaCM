package com.sequoiacm.scmfile.serial;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmFileBasicInfo;
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
 * @Description:SCM-717 : 并发读、写、查询文件（大并发） 1、写入部分文件（用于读和查询）；
 *                      2、并发做写、读、查询操作，并发数如500并发，具体操作如下： 1）循环写文件，文件大小约300K；
 *                      2）循环读取已存在的文件； 3）带条件查询已存在的文件，并检查结果集正确性；
 *                      3、检查新写入的文件属性、文件内容正确性； 4、检查读取文件内容正确性；
 * @author fanyu
 * @Date:2017年8月14日
 * @version:1.0
 */
public class WRQScmFile717 extends TestScmBase {
    private static final String author = "WRQScmFile717";
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private int fileNum = 1;
    private ScmId fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( wsp, cond );
            BSONObject cond1 = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author + "_W" )
                    .get();
            ScmFileUtils.cleanFile( wsp, cond1 );

            prepareFiles();
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        try {
            WriteScmFile wThread = new WriteScmFile();
            wThread.start( 10 );

            ReadScmFile rThread = new ReadScmFile();
            rThread.start( 10 );

            QueryScmFile qThread = new QueryScmFile();
            qThread.start( 10 );

            Assert.assertTrue( wThread.isSuccess(), wThread.getErrorMsg() );
            Assert.assertTrue( rThread.isSuccess(), rThread.getErrorMsg() );
            Assert.assertTrue( qThread.isSuccess(), qThread.getErrorMsg() );

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
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();
                ScmFileUtils.cleanFile( wsp, cond );
                BSONObject cond1 = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR )
                        .is( author + "_W" ).get();
                ScmFileUtils.cleanFile( wsp, cond1 );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {

        }
    }

    private void prepareFiles() {
        ScmSession sessionA = null;
        try {
            sessionA = TestScmTools.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), sessionA );
            for ( int i = 0; i < fileNum; ++i ) {
                ScmFile scmfile = ScmFactory.File.createInstance( ws );
                scmfile.setContent( filePath );
                scmfile.setFileName( author + "_" + UUID.randomUUID() );
                scmfile.setAuthor( author );
                fileId = scmfile.save();
            }
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void checkRead( ScmFile file, ScmId fileId ) {
        try {
            SiteWrapper[] expSites = { site };
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                    filePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void checkQuery( ScmCursor< ScmFileBasicInfo > cursor ) {
        int size = 0;
        ScmFileBasicInfo fileInfo;
        try {
            while ( cursor.hasNext() ) {
                fileInfo = cursor.getNext();
                // check results
                Assert.assertEquals( fileInfo.getMinorVersion(), 0 );
                Assert.assertEquals( fileInfo.getMajorVersion(), 1 );
                size++;
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != cursor ) {
                cursor.close();
            }
        }
        Assert.assertEquals( size, fileNum );
    }

    private class WriteScmFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                // login
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                // write
                for ( int i = 0; i < fileNum; i++ ) {
                    ScmFile file = ScmFactory.File.createInstance( ws );
                    file.setContent( filePath );
                    file.setFileName( author + "_" + UUID.randomUUID() );
                    file.setAuthor( author + "_W" );
                    ScmId fileId = file.save();
                    file = ScmFactory.File.getInstance( ws, fileId );
                    String downloadPath = TestTools.LocalFile
                            .initDownloadPath( localPath,
                                    TestTools.getMethodName(),
                                    Thread.currentThread().getId() );
                    file.getContentFromLocalSite( downloadPath );
                    checkWrite( file, fileId, downloadPath );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }

        private void checkWrite( ScmFile file, ScmId fileId,
                String downloadPath ) {
            try {
                Assert.assertEquals( file.getWorkspaceName(), wsp.getName() );
                Assert.assertEquals( file.getAuthor(), author + "_W" );
                Assert.assertEquals( file.getSize(), fileSize );
                Assert.assertEquals( file.getMinorVersion(), 0 );
                Assert.assertEquals( file.getMajorVersion(), 1 );
                Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
                Assert.assertNotNull( file.getCreateTime().getTime() );

                SiteWrapper[] expSites = { site };
                ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                        filePath );
            } catch ( Exception e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }
    }

    private class ReadScmFile extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );

                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile
                        .initDownloadPath( localPath, TestTools.getMethodName(),
                                Thread.currentThread().getId() );
                file.getContent( downloadPath );
                checkRead( file, fileId );
            } catch ( Exception e ) {
                Assert.fail( e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class QueryScmFile extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                // listInstance
                ScopeType scopeType = ScopeType.SCOPE_CURRENT;
                BSONObject condition = new BasicBSONObject();
                condition.put( ScmAttributeName.File.AUTHOR, author );
                ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File
                        .listInstance( ws, scopeType, condition );
                checkQuery( cursor );
            } finally {
                if ( null != session ) {
                    session.close();
                }
            }
        }
    }
}
