package com.sequoiacm.net.task;

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
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @Description: SCM-754 : 异步缓存到分中心并读取文件，读取后清理文件 1、在分中心A异步缓存单个文件； 2、在分中心A读取该文件；
 *               3、在分中心A清理该文件； 4、检查执行结果正确性；
 * @author fanyu
 * @Date:2017年8月11日
 * @version:1.0
 */
public class AsyncCacheThenReadThenClean754 extends TestScmBase {
    private static final String author = "CacheAndReadAndClean754";
    private boolean runSuccess = false;
    private int fileSize = 1024 * 1;
    private File localPath = null;
    private String filePath = null;
    private ScmId fileId = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId taskId = null;

    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private WsWrapper ws_T = null;

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

            ws_T = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getSortSites( ws_T );
            sourceSite = siteList.get( 0 );
            targetSite = siteList.get( 1 );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            ScmFileUtils.cleanFile( ws_T, cond );

            session = TestScmTools.createSession( sourceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), session );
            write();
        } catch ( IOException | ScmException e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        cache();
        read();
        clean();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                BSONObject cond = ScmQueryBuilder
                        .start( ScmAttributeName.File.AUTHOR ).is( author )
                        .get();
                ScmFileUtils.cleanFile( ws_T, cond );
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void cache() throws Exception {
        ScmSession sessionT = TestScmTools.createSession( targetSite );
        try {
            ScmWorkspace ws1 = ScmFactory.Workspace
                    .getWorkspace( ws_T.getName(), sessionT );
            ScmFactory.File.asyncCache( ws1, fileId );
            // checkResult();
            SiteWrapper[] expSiteList = { sourceSite, targetSite };
            ScmTaskUtils
                    .waitAsyncTaskFinished( ws, fileId, expSiteList.length );
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                    filePath );
        } finally {
            if ( sessionT != null ) {
                sessionT.close();
            }
        }

    }

    private void read() {
        ScmFile file;
        try {
            file = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile
                    .initDownloadPath( localPath, TestTools.getMethodName(),
                            Thread.currentThread().getId() );
            file.getContent( downloadPath );
            checkResult( file, fileId, ws_T.getName(), downloadPath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void clean() {
        try {
            BSONObject condition = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( author ).get();
            taskId = ScmSystem.Task.startCleanTask( ws, condition );
            ScmTaskUtils.waitTaskFinish( session, taskId );
            ScmTask taskInfo = ScmSystem.Task.getTask( session, taskId );
            checkTaskResult( taskInfo, condition );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void write() throws ScmException {
        // write
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( author + "_" + UUID.randomUUID() );
        file.setAuthor( author );
        fileId = file.save();
    }

    private void checkResult( ScmFile file, ScmId fileId, String wsName,
            String downloadPath ) {
        try {
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
            Assert.assertEquals( file.getWorkspaceName(), wsName );
            Assert.assertEquals( file.getFileId(), fileId );
            Assert.assertEquals( file.getAuthor(), author );
            Assert.assertEquals( file.getSize(), fileSize );
            Assert.assertEquals( file.getMinorVersion(), 0 );
            Assert.assertEquals( file.getMajorVersion(), 1 );
            Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
            Assert.assertNotNull( file.getCreateTime().getTime() );
            SiteWrapper[] expSiteList = { sourceSite, targetSite };
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                    filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void checkTaskResult( ScmTask taskInfo, BSONObject condition ) {
        try {
            SiteWrapper[] expSiteList = { targetSite };
            ScmFileUtils.checkMetaAndData( ws_T, fileId, expSiteList, localPath,
                    filePath );
            Assert.assertEquals( taskInfo.getProgress(), 100 );
            Assert.assertEquals( taskInfo.getRunningFlag(),
                    CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
            Assert.assertEquals( taskInfo.getType(),
                    CommonDefine.TaskType.SCM_TASK_CLEAN_FILE );
            Assert.assertEquals( taskInfo.getWorkspaceName(), ws_T.getName() );
            Assert.assertEquals( taskInfo.getContent(), condition );
            Assert.assertNotNull( taskInfo.getStartTime() );
            Assert.assertNotNull( taskInfo.getStopTime() );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }
}
