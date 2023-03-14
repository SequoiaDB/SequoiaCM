package com.sequoiacm.task.concurrent;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sequoiacm.client.common.ScmType;
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
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @FileName SCM-440: 并发停止不同任务
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、在分中心A开始多个迁移任务，迁移多个文件； 2、迁移部分文件后，并发分中心B停止不同任务； 3、检查执行结果正确性； 备注：
 * 因为虚机空间太小，所以我将原本的3ws并发改为2ws并发，若未来空间条件允许，就把ws3的注释打开吧
 */

public class Transfer_stopRunningTask440 extends TestScmBase {
    private final String authorName = "case440";
    private final int fileSize = 1024 * 1024;
    private final int fileNum = 50;
    private boolean runSuccess = false;
    private ScmSession sessionM = null; // mainCenter
    private ScmSession sessionA = null; // subCenterA
    private ScmSession sessionB = null; // subCenterB
    private ScmWorkspace ws1 = null;
    private ScmWorkspace ws2 = null;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();
    private File localPath = null;
    private String filePath = null;

    private Date expStopTime = null;
    private ScmId taskId1 = null;
    private ScmId taskId2 = null;

    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branceSiteList = new ArrayList< SiteWrapper >();
    private List< WsWrapper > ws_TList = new ArrayList< WsWrapper >();

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            for ( int i = 0; i < fileNum; i++ ) {
                TestTools.LocalFile.createFile( filePath, fileSize + i );
            }

            rootSite = ScmInfo.getRootSite();
            branceSiteList = ScmInfo.getBranchSites( 2 );
            ws_TList = ScmInfo.getWss( 2 );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_TList.get( 0 ), cond );
            ScmFileUtils.cleanFile( ws_TList.get( 1 ), cond );

            sessionM = ScmSessionUtils.createSession( rootSite );
            sessionA = ScmSessionUtils.createSession( branceSiteList.get( 0 ) );
            sessionB = ScmSessionUtils.createSession( branceSiteList.get( 1 ) );
            ws1 = ScmFactory.Workspace
                    .getWorkspace( ws_TList.get( 0 ).getName(), sessionA );
            ws2 = ScmFactory.Workspace
                    .getWorkspace( ws_TList.get( 1 ).getName(), sessionA );

            prepareFiles( ws1 );
            prepareFiles( ws2 );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void testTransfer() throws Exception {
        ScmQueryBuilder basicCond = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName );

        BSONObject cond1 = basicCond.and( ScmAttributeName.File.TITLE )
                .is( ws1.getName() ).get();
        taskId1 = ScmSystem.Task.startTransferTask( ws1, cond1,
                ScmType.ScopeType.SCOPE_CURRENT, rootSite.getSiteName() );
        ScmSystem.Task.stopTask( sessionB, taskId1 );

        BSONObject cond2 = basicCond.and( ScmAttributeName.File.TITLE )
                .is( ws2.getName() ).get();
        taskId2 = ScmSystem.Task.startTransferTask( ws2, cond2,
                ScmType.ScopeType.SCOPE_CURRENT, rootSite.getSiteName() );
        ScmSystem.Task.stopTask( sessionB, taskId2 );

        waitTaskStop( taskId1 );
        waitTaskStop( taskId2 );

        checkTaskAttr( sessionA, taskId1 );
        checkTaskAttr( sessionA, taskId2 );

        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < fileIdList.size(); i++ ) {
                    ScmId fileId = fileIdList.get( i );
                    if ( i < fileIdList.size() / 2 ) {
                        ScmFactory.File.deleteInstance( ws1, fileId, true );
                    } else {
                        ScmFactory.File.deleteInstance( ws2, fileId, true );
                    }
                }
                TestSdbTools.Task.deleteMeta( taskId1 );
                TestSdbTools.Task.deleteMeta( taskId2 );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }

    private void prepareFiles( ScmWorkspace ws ) throws Exception {
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            scmfile.setTitle( ws.getName() ); // 3 workspaces
            scmfile.setContent( filePath );
            fileIdList.add( scmfile.save() );
        }
    }

    private void waitTaskStop( ScmId taskId ) throws ScmException {
        Date stopTime = null;
        while ( stopTime == null ) {
            stopTime = ScmSystem.Task.getTask( sessionA, taskId ).getStopTime();
        }
    }

    private void checkTaskAttr( ScmSession session, ScmId taskId )
            throws ScmException {
        ScmTask task = ScmSystem.Task.getTask( session, taskId );
        Assert.assertEquals( task.getRunningFlag(),
                CommonDefine.TaskRunningFlag.SCM_TASK_CANCEL ); // 4:

        Date actStopTime = null;
        while ( null == actStopTime ) {
            actStopTime = ScmSystem.Task.getTask( session, taskId )
                    .getStopTime();
        }
        expStopTime = new Date();

        long acceptableOffset = 180 * 1000; // 3m
        if ( Math.abs( actStopTime.getTime()
                - expStopTime.getTime() ) > acceptableOffset ) {
            Assert.fail( "actStopTime: " + actStopTime + ", expStopTime: "
                    + expStopTime + ", stopTime is not reasonable" );

        }
    }
}