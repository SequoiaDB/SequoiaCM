package com.sequoiacm.asynctask.concurrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
 * @Description:SCM-753 : 分中心A的2个节点并发异步缓存、读取不同文件 1、分中心A的2个节点并发异步缓存、读取不同文件；
 *                      2、检查执行结果正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class AsyncCacheAndReadDiffFile753 extends TestScmBase {
    private boolean runSuccess = false;
    private int fileSize = 1024 * new Random().nextInt( 1025 );
    private File localPath = null;
    private String filePath = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private int fileNum = 2;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String author = "DiffNodeCacheAndReadDiffFile753";

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    // private List<NodeWrapper> nodeList = new ArrayList<NodeWrapper>();
    private WsWrapper ws_T = null;

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
            branceSite = ScmInfo.getBranchSite();
            // nodeList = branceSite.getNodes(2);
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            session = TestScmTools.createSession( rootSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );
            write();
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() {
        try {
            CacheFile cThread1 = new CacheFile( branceSite,
                    fileIdList.get( 0 ) );
            cThread1.start( 10 );

            ReadFile rThred1 = new ReadFile( branceSite, fileIdList.get( 1 ) );
            rThred1.start( 10 );

            if ( !( cThread1.isSuccess() && rThred1.isSuccess() ) ) {
                Assert.fail( cThread1.getErrorMsg() + rThred1.getErrorMsg() );
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
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != session ) {
                session.close();
            }

        }
    }

    private void write() {
        try {
            for ( int i = 0; i < fileNum; i++ ) {
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setContent( filePath );
                file.setAuthor( author );
                file.setFileName( author + "_" + UUID.randomUUID() );
                ScmId fileId = file.save();
                fileIdList.add( fileId );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void checkResult() {
        try {
            SiteWrapper[] expSiteList = { rootSite, branceSite };
            ScmTaskUtils.waitAsyncTaskFinished( ws, fileIdList.get( 0 ),
                    expSiteList.length );
            ScmFileUtils.checkMetaAndData( ws_T, fileIdList, expSiteList,
                    localPath, filePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private class CacheFile extends TestThreadBase {
        private SiteWrapper site;
        private ScmId fileId;

        public CacheFile( SiteWrapper site, ScmId fileId ) {
            this.site = site;
            this.fileId = fileId;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );
                ScmFactory.File.asyncCache( ws, fileId );
            } catch ( Exception e ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            } finally {
                if ( null != session ) {
                    session.close();
                }
            }
        }
    }

    private class ReadFile extends TestThreadBase {
        private SiteWrapper site;
        private ScmId fileId;

        public ReadFile( SiteWrapper site, ScmId fileId ) {
            this.site = site;
            this.fileId = fileId;
        }

        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            try {
                session = TestScmTools.createSession( site );
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( ws_T.getName(), session );
                ScmFile file = ScmFactory.File.getInstance( ws, fileId );
                String downloadPath = TestTools.LocalFile.initDownloadPath(
                        localPath, TestTools.getMethodName(),
                        Thread.currentThread().getId() );
                file.getContent( downloadPath );
                Assert.assertEquals( TestTools.getMD5( downloadPath ),
                        TestTools.getMD5( filePath ) );
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
