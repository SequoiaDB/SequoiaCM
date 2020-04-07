package com.sequoiacm.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-2202 : 设置maxExecTime,清理文件
 * @Author fanyu
 * @Date 2018-09-05
 * @Version 1.00
 */

public class Clean_maxExecTime2202 extends TestScmBase {
    private boolean runSuccess = false;

    private int fileSize = 1024;
    private int fileNum = 10;
    private File localPath = null;
    private String filePath = null;
    private String authorName = "Clean_maxExecTime2202";
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private List< ScmId > taskIdList = new ArrayList< ScmId >();
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    private SiteWrapper rootSite = null;
    private SiteWrapper branceSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );
            // login in
            sessionA = TestScmTools.createSession( branceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        try {
            //set maxExecTime = 0;
            prepareFiles();
            long maxExecTime = 0L;
            startTask( maxExecTime );
            ScmTaskUtils.waitTaskFinish( sessionA, taskIdList.get( 0 ) );
            waitTaskStop( taskIdList.get( 0 ) );
            checkCleanTaskResult( taskIdList.get( 0 ), maxExecTime,
                    fileIdList.subList( 0, fileNum ) );

            //set maxExecTime < 0;
            prepareFiles();
            maxExecTime = -1L;
            startTask( maxExecTime );
            ScmTaskUtils.waitTaskFinish( sessionA, taskIdList.get( 1 ) );
            waitTaskStop( taskIdList.get( 1 ) );
            checkCleanTaskResult( taskIdList.get( 1 ), maxExecTime,
                    fileIdList.subList( fileNum, 2 * fileNum ) );

            //set maxExecTime > 0;
            prepareFiles();
            maxExecTime = 1000 * 60 * 60L;
            startTask( maxExecTime );
            ScmTaskUtils.waitTaskFinish( sessionA, taskIdList.get( 2 ) );
            waitTaskStop( taskIdList.get( 2 ) );
            checkCleanTaskResult( taskIdList.get( 2 ), maxExecTime,
                    fileIdList.subList( 2 * fileNum, 3 * fileNum ) );
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
                for ( ScmId fileId : fileIdList ) {
                    ScmFactory.File.deleteInstance( wsA, fileId, true );
                }
                for ( ScmId taskId : taskIdList ) {
                    TestSdbTools.Task.deleteMeta( taskId );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( BaseException | ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }

        }
    }

    private void prepareFiles() throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmId fileId = writeFileFromSubCenterB();
            readFileFromMainCenter( fileId );
            fileIdList.add( fileId );
        }
    }

    private ScmId writeFileFromSubCenterB() {
        ScmId fileId = null;
        try {
            ScmFile scmfile = ScmFactory.File.createInstance( wsA );
            scmfile.setContent( filePath );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            fileId = scmfile.save();
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
        return fileId;
    }

    private void readFileFromMainCenter( ScmId fileId ) throws Exception {
        ScmSession sessionM = null;
        try {
            // login
            sessionM = TestScmTools.createSession( rootSite );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( ws_T.getName(), sessionM );
            // read content
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile
                    .initDownloadPath( localPath, TestTools.getMethodName(),
                            Thread.currentThread().getId() );
            file.getContent( downloadPath );
        } finally {
            if ( sessionM != null )
                sessionM.close();
        }
    }

    private void checkCleanTaskResult( ScmId taskId, long maxExecTime,
            List< ScmId > fileIdList ) {
        try {
            // check task Attribute
            ScmTask task = ScmSystem.Task.getTask( sessionA, taskId );
            Assert.assertEquals( task.getId(), taskId );
            Assert.assertEquals( task.getProgress(), 100 );
            Assert.assertEquals( task.getRunningFlag(),
                    CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
            Assert.assertEquals( task.getType(),
                    CommonDefine.TaskType.SCM_TASK_CLEAN_FILE );
            Assert.assertEquals( task.getWorkspaceName(), ws_T.getName() );
            Assert.assertEquals( task.getMaxExecTime(), maxExecTime );
            // check meta data
            SiteWrapper[] expSiteList = { rootSite };
            ScmFileUtils
                    .checkMetaAndData( ws_T, fileIdList, expSiteList, localPath,
                            filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void startTask( long maxExecTime ) {
        try {
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmId taskId = ScmSystem.Task
                    .startCleanTask( wsA, cond, ScopeType.SCOPE_CURRENT,
                            maxExecTime );
            taskIdList.add( taskId );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    private void waitTaskStop( ScmId taskId ) throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( sessionA, taskId ).getStopTime();
        }
    }
}