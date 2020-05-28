package com.sequoiacm.asynctask.serial;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
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
 * @Description: SCM-751 : 并发异步缓存不同文件（大并发） 1、并发做如下操作，并发数如500并发（每个并发包含如下3个步骤）：
 *               1）主中心写入文件； 2）分中心异步缓存该文件，检查文件元数据、文件内容正确性； 3）删除文件该文件；
 *               2、检查执行结果正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class AsyncCacheAndDelete751 extends TestScmBase {
    private static String filePath = null;
    private boolean runSuccess = false;
    private int fileSize = 1024 * 1;
    private File localPath = null;
    private String author = "AsyncCacheAndDelete751";

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        // filePathList.add(filePath);
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( ws_T, cond );
        } catch ( IOException | ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            WCDFile wcdThread = new WCDFile();
            wcdThread.start( 20 );
            Assert.assertTrue( wcdThread.isSuccess(), wcdThread.getErrorMsg() );
            checkDeleteResult();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException e ) {
            Assert.fail( e.getMessage() );
        } finally {

        }
    }

    private void checkDeleteResult() {
        ScmSession session = null;
        try {
            session = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                    session );
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            long cnt = ScmFactory.File.countInstance( ws,
                    ScopeType.SCOPE_CURRENT, cond );
            Assert.assertEquals( cnt, 0 );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private class WCDFile extends TestThreadBase {

        @Override
        public void exec() throws Exception {
            ScmId fileId = write();
            cache( fileId );
            delete( fileId );
        }

        public void cache( ScmId fileId ) throws InterruptedException {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branceSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );
                ScmFactory.File.asyncCache( ws, fileId );
                SiteWrapper[] siteList = { rootSite, branceSite };
                ScmTaskUtils.waitAsyncTaskFinished( ws, fileId,
                        siteList.length );
                ScmFileUtils.checkMetaAndData( ws_T, fileId, siteList,
                        localPath, filePath );
            } catch ( Exception e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }

        public void delete( ScmId fileId ) {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( branceSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );
                // delete
                ScmFactory.File.deleteInstance( ws, fileId, true );
            } catch ( ScmException e ) {
                Assert.fail( e.getMessage() );
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }

        private ScmId write() {
            ScmSession session = null;
            ScmId fileId = null;
            try {
                session = TestScmTools.createSession( rootSite );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setContent( filePath );
                file.setFileName( author + "_" + UUID.randomUUID() );
                file.setAuthor( author );
                fileId = file.save();
            } catch ( ScmException e ) {
                Assert.fail( e.getMessage() );
            } finally {
                if ( null != session ) {
                    session.close();
                }
            }
            return fileId;
        }
    }
}
