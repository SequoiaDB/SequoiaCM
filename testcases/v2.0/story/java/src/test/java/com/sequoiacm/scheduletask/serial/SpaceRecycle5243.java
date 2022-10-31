package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmSpaceRecycleScope;
import com.sequoiacm.client.element.ScmSpaceRecyclingTaskConfig;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Set;

/**
 * @Descreption SCM-5243:创建空间回收任务，起始点与时间范围测试
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class SpaceRecycle5243 extends TestScmBase {
    private String fileName = "file5243_";
    private String fileAuthor = "author5243";
    private String wsName = "ws_5243";
    private long year = 365L * 24L * 60L * 60L * 1000L;
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmWorkspace ws;
    private BSONObject queryCond;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private int recycleCSNum = 5;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ws = ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        createNullCS();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 清理6年前的空表
        ScmSpaceRecyclingTaskConfig conf = new ScmSpaceRecyclingTaskConfig();
        conf.setRecycleScope( ScmSpaceRecycleScope.mothBefore( 12 * 6 ) );
        conf.setWorkspace( ws );
        ScmId taskID = ScmSystem.Task.startSpaceRecyclingTask( conf );
        ScmTaskUtils.waitTaskFinish( session, taskID );
        ScmTask task = ScmSystem.Task.getTask( session, taskID );
        Assert.assertNull( task.getExtraInfo() );

        // 清理2年前的空表
        int yearBefore = 2;
        conf.setRecycleScope(
                ScmSpaceRecycleScope.mothBefore( 12 * yearBefore ) );
        taskID = ScmSystem.Task.startSpaceRecyclingTask( conf );
        ScmTaskUtils.waitTaskFinish( session, taskID );
        Set< String > expCleanCSNames = ScmScheduleUtils.initLobCSName( wsName,
                ScmShardingType.YEAR,
                System.currentTimeMillis() - year * yearBefore,
                recycleCSNum - yearBefore );
        task = ScmSystem.Task.getTask( session, taskID );
        Object[] taskExtraInfo = ScmScheduleUtils.getTaskExtraInfo( task );
        Assert.assertEqualsNoOrder( taskExtraInfo, expCleanCSNames.toArray(),
                "act:" + Arrays.toString( taskExtraInfo ) + ",exp:"
                        + expCleanCSNames );
        for ( String csName : expCleanCSNames ) {
            Assert.assertFalse(
                    ScmScheduleUtils.checkLobCS( rootSite, csName ) );
        }
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmWorkspaceUtil.deleteWs( wsName, session );
            } finally {
                session.close();
            }
        }
    }

    private void createNullCS() throws Exception {
        ScmScheduleUtils.cleanNullCS( session, wsName );
        Calendar instance = Calendar.getInstance();
        for ( int i = 0; i < recycleCSNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setCreateTime( instance.getTime() );
            file.setAuthor( fileAuthor );
            file.setFileName( fileName + i );
            file.save();
            instance.add( Calendar.YEAR, -1 );
        }
        ScmFileUtils.cleanFile( wsName, queryCond );
    }
}