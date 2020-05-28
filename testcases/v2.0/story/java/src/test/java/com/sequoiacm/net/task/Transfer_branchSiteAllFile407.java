package com.sequoiacm.net.task;

import java.io.File;
import java.text.SimpleDateFormat;
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
import com.sequoiacm.testcommon.Ssh;
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
 * @FileName SCM-407: 迁移分中心所有文件
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、在分中心A写多个文件； 2、在分中心A开始迁移任务，指定迁移条件匹配分中心所有文件； 3、检查迁移任务执行结果；
 */

public class Transfer_branchSiteAllFile407 extends TestScmBase {
    private final int fileSize = 200 * 1024;
    private final int fileNum = 100;
    private boolean runSuccess = false;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    private File localPath = null;
    private String filePath = null;
    private String authorName = "TransferAllFile407";
    private Date expStartTime = null;
    private Date expStopTime = null;

    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmId taskId = null;

    private SiteWrapper sourceSite = null;
    private SiteWrapper targetSite = null;
    private WsWrapper ws_T = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            ws_T = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( ws_T );
            sourceSite = siteList.get( 0 );
            targetSite = siteList.get( 1 );

            sessionA = TestScmTools.createSession( sourceSite );
            wsA = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            prepareFiles( wsA );
        } catch ( Exception e ) {
            if ( sessionA != null ) {
                sessionA.close();
            }
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            taskId = transferAllFile( wsA );
            ScmTaskUtils.waitTaskFinish( sessionA, taskId );
            expStopTime = getDate();
            checkTransfered();
            checkTaskAttr( sessionA, taskId );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( int i = 0; i < fileNum; ++i ) {
                    ScmFactory.File.deleteInstance( wsA, fileIdList.get( i ),
                            true );
                }
                TestTools.LocalFile.removeFile( localPath );
                TestSdbTools.Task.deleteMeta( taskId );
            }
        } catch ( BaseException | ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private Date getDate() throws Exception {
        Ssh ssh = null;
        String localDate = null;
        String host = sourceSite.getNode().getHost();
        try {
            ssh = new Ssh( host );
            ssh.exec( "date '+%Y-%m-%d %H:%M:%S'" );
            localDate = ssh.getStdout();
            System.out
                    .println( "host = " + host + ", localDate = " + localDate );
        } finally {
            if ( ssh != null ) {
                ssh.disconnect();
            }
        }
        SimpleDateFormat format = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
        return format.parse( localDate );
    }

    private void prepareFiles( ScmWorkspace ws ) throws Exception {
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile scmfile = ScmFactory.File.createInstance( ws );
            scmfile.setContent( filePath );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setAuthor( authorName );
            fileIdList.add( scmfile.save() );
        }
    }

    private ScmId transferAllFile( ScmWorkspace ws ) throws Exception {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        expStartTime = getDate();
        return ScmSystem.Task.startTransferTask( ws, condition,
                ScopeType.SCOPE_CURRENT, targetSite.getSiteName() );
    }

    private void checkTransfered() {
        try {
            SiteWrapper[] expSiteList = { sourceSite, targetSite };
            ScmFileUtils.checkMetaAndData( ws_T, fileIdList, expSiteList,
                    localPath, filePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    private void checkTaskAttr( ScmSession session, ScmId taskId )
            throws ScmException {
        ScmTask task = ScmSystem.Task.getTask( session, taskId );
        Assert.assertEquals( task.getId(), taskId );
        Assert.assertEquals( task.getProgress(), 100 );
        Assert.assertEquals( task.getRunningFlag(),
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
        Assert.assertEquals( task.getType(),
                CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
        Assert.assertEquals( task.getWorkspaceName(), wsA.getName() );

        Date actStartTime = task.getStartTime();
        Date actStopTime = task.getStopTime();
        if ( actStartTime.getTime() > actStopTime.getTime() ) {
            Assert.fail( "taskId :" + taskId.get() + "startTime: "
                    + actStartTime + "stopTime: " + actStopTime
                    + ", startTime shouldn't greater than stopTime!" );
        }
        long acceptableOffset = 2000 * 1000; // unit: ms
        if ( Math.abs( actStartTime.getTime()
                - expStartTime.getTime() ) > acceptableOffset ) {
            Assert.fail( "taskId :" + taskId.get() + "actStartTime: "
                    + actStartTime + ", expStartTime: " + expStartTime
                    + ", startTime is not reasonable" );
        }
        if ( Math.abs( actStopTime.getTime()
                - expStopTime.getTime() ) > acceptableOffset ) {
            Assert.fail( "taskId :" + taskId.get() + "actStopTime: "
                    + actStopTime + "expStopTime: " + expStopTime
                    + ", stopTime is not reasonable" );
        }
    }
}