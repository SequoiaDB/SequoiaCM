package com.sequoiacm.task;

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
import com.sequoiacm.common.MimeType;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.Ssh;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiadb.exception.BaseException;

/**
 * @FileName SCM-463: 迁移条件测试
 * @Author linsuqiang
 * @Date 2017-06-12
 * @Version 1.00
 */

/*
 * 1、在分中心B写多个文件； 2、在分中心B开始迁移任务，指定迁移条件匹配分中心部分文件，迁移条件覆盖：
 * 1）迁移条件覆盖分别覆盖所有文件属性（可以单个或组合覆盖；） 2）迁移条件按最近访问时间匹配（site_list.last_access_time）；
 * 3）使用ScmQueryBuilder构造BSONObject迁移条件； 2、检查迁移任务执行结果；
 */

public class Transfer_transferCond463 extends TestScmBase {
    private final int fileSize = 200 * 1024;
    private final int fileNum = 1;
    private boolean runSuccess = false;
    private List< ScmId > fileIdList = new ArrayList< ScmId >();

    private File localPath = null;
    private String filePath = null;

    private Date expStartTime = null;
    private Date expStopTime = null;

    private ScmSession sessionA = null;
    private String authorName = "case463";
    private ScmWorkspace ws = null;
    private ScmId taskId = null;

    private SiteWrapper branceSite = null;
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

            branceSite = ScmInfo.getBranchSite();
            ws_T = ScmInfo.getWs();

            sessionA = TestScmTools.createSession( branceSite );
            ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(), sessionA );

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).is( authorName )
                    .get();
            ScmFileUtils.cleanFile( ws_T, cond );

            prepareFiles( ws );
        } catch ( Exception e ) {
            if ( sessionA != null ) {
                sessionA.close();
            }
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void testAllAttr() throws Exception {
        try {
            // search file created in last 1 hour
            long nowTime = getDate().getTime();
            long beginTime = nowTime - ( 60 * 60 * 1000 ); // 1 hour before
            long endTime = nowTime + ( 10 * 60 * 1000 ); // 10min after

            ScmQueryBuilder siteIdIs2 = ScmQueryBuilder
                    .start( ScmAttributeName.File.SITE_ID )
                    .is( branceSite.getSiteId() );
            String lastAccessTime = ScmAttributeName.File.LAST_ACCESS_TIME;
            BSONObject timeGt = siteIdIs2.and( lastAccessTime )
                    .greaterThan( beginTime ).get();
            BSONObject timeLte = siteIdIs2.and( lastAccessTime )
                    .lessThanEquals( endTime ).get();

            ScmFile file = ScmFactory.File.getInstance( ws,
                    fileIdList.get( 0 ) );
            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR )
                    .is( file.getAuthor() )
                    .and( ScmAttributeName.File.BATCH_ID ).exists( 1 )
                    .and( ScmAttributeName.File.CREATE_TIME ).exists( 1 )
                    .and( ScmAttributeName.File.FILE_ID ).exists( 1 )
                    .and( ScmAttributeName.File.FILE_NAME )
                    .is( file.getFileName() )
                    .and( ScmAttributeName.File.MAJOR_VERSION ).exists( 1 )
                    .and( ScmAttributeName.File.MIME_TYPE )
                    .is( file.getMimeType() )
                    .and( ScmAttributeName.File.MINOR_VERSION ).exists( 1 )
                    .and( ScmAttributeName.File.PROPERTIES ).exists( 1 )
                    .and( ScmAttributeName.File.PROPERTY_TYPE ).exists( 0 )
                    .and( ScmAttributeName.File.SITE_LIST ).elemMatch( timeGt )
                    .and( ScmAttributeName.File.SITE_LIST ).elemMatch( timeLte )
                    .and( ScmAttributeName.File.SIZE ).exists( 1 )
                    .and( ScmAttributeName.File.TITLE ).is( file.getTitle() )
                    .and( ScmAttributeName.File.UPDATE_TIME ).exists( 1 )
                    .and( ScmAttributeName.File.UPDATE_USER )
                    .is( TestScmBase.scmUserName )
                    .and( ScmAttributeName.File.USER )
                    .is( TestScmBase.scmUserName ).get();
            System.out.println( "cond = " + cond );
            long actCount = ScmFactory.File.countInstance( ws,
                    ScopeType.SCOPE_CURRENT, cond );
            System.out
                    .println( "======================actCount = " + actCount );
            taskId = transferByCond( sessionA, cond );
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
                    ScmFactory.File.deleteInstance( ws, fileIdList.get( i ),
                            true );
                    ;
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
        try {
            ssh = new Ssh( branceSite.getNode().getHost() );
            ssh.exec( "date '+%Y-%m-%d %H:%M:%S'" );
            localDate = ssh.getStdout();
            System.out.println( "host = " + branceSite.getNode().getHost()
                    + ", localDate = " + localDate );
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
            scmfile.setAuthor( authorName );
            scmfile.setFileName( authorName + "_" + UUID.randomUUID() );
            scmfile.setMimeType( MimeType.JPEG );
            scmfile.setTitle( authorName );
            scmfile.setContent( filePath );
            ScmId fileId = scmfile.save();
            fileIdList.add( fileId );
        }
    }

    private ScmId transferByCond( ScmSession session, BSONObject cond )
            throws Exception {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( ws_T.getName(),
                session );
        expStartTime = getDate();
        ScmId taskId = ScmSystem.Task.startTransferTask( ws, cond );
        return taskId;
    }

    private void checkTransfered() {
        try {
            SiteWrapper rootSite = ScmInfo.getRootSite();
            SiteWrapper[] expSiteList = { rootSite, branceSite };
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
        Assert.assertNotNull( task.getServerId(),
                "serverId : " + task.getServerId() );
        Assert.assertEquals( task.getProgress(), 100 );
        Assert.assertEquals( task.getRunningFlag(),
                CommonDefine.TaskRunningFlag.SCM_TASK_FINISH );
        Assert.assertEquals( task.getType(),
                CommonDefine.TaskType.SCM_TASK_TRANSFER_FILE );
        Assert.assertEquals( task.getWorkspaceName(), ws.getName() );

        Date actStartTime = task.getStartTime();
        Date actStopTime = task.getStopTime();
        if ( actStartTime.getTime() > actStopTime.getTime() ) {
            Assert.fail( "taskId :" + taskId.get() + "startTime: "
                    + actStartTime + "stopTime: " + actStopTime
                    + ", startTime shouldn't greater than stopTime!" );
        }
        long acceptableOffset = 100000; // 100s
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