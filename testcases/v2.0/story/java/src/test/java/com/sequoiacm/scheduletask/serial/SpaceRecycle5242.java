package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmSpaceRecycleScope;
import com.sequoiacm.client.element.ScmSpaceRecyclingTaskConfig;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
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
import java.util.Date;
import java.util.Set;

/**
 * @Descreption SCM-5242:创建空间回收任务，回收工作区在某个站点上的空表
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class SpaceRecycle5242 extends TestScmBase {
    private String fileName = "file5242_";
    private String fileAuthor = "author5242";
    private String wsName = "ws_5242";
    private SiteWrapper rootSite = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM;
    private BSONObject queryCond;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private int recycleCSNum = 10;
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
        sessionM = TestScmTools.createSession( rootSite );
        ScmWorkspaceUtil.deleteWs( wsName, sessionM );
        wsM = ScmWorkspaceUtil.createWS( sessionM, wsName,
                ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( sessionM, wsName );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileAuthor ).get();
        ScmFileUtils.cleanFile( wsName, queryCond );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    public void test() throws Exception {
        // 全部表都为空表
        createNullCS();
        ScmSpaceRecyclingTaskConfig conf = new ScmSpaceRecyclingTaskConfig();
        conf.setRecycleScope( ScmSpaceRecycleScope.mothBefore( 0 ) );
        conf.setWorkspace( wsM );
        ScmId taskID = ScmSystem.Task.startSpaceRecyclingTask( conf );
        ScmTaskUtils.waitTaskFinish( sessionM, taskID );
        ScmTask task = ScmSystem.Task.getTask( sessionM, taskID );
        Set< String > expCleanCSNames = ScmScheduleUtils.initLobCSName( wsName,
                ScmShardingType.YEAR, System.currentTimeMillis(),
                recycleCSNum );
        Object[] taskExtraInfo = ScmScheduleUtils.getTaskExtraInfo( task );
        Assert.assertEqualsNoOrder( taskExtraInfo, expCleanCSNames.toArray(),
                "act:" + Arrays.toString( taskExtraInfo ) + ",exp:"
                        + expCleanCSNames );

        // 写入一个文件占用一张表，其余为空表
        createNullCS();
        long year = 365L * 24L * 60L * 60L * 1000L;
        long fileACreate_time = System.currentTimeMillis() - year;
        createFile( fileACreate_time );
        taskID = ScmSystem.Task.startSpaceRecyclingTask( conf );
        ScmTaskUtils.waitTaskFinish( sessionM, taskID );
        task = ScmSystem.Task.getTask( sessionM, taskID );
        taskExtraInfo = ScmScheduleUtils.getTaskExtraInfo( task );
        String fileBLobCS = ScmScheduleUtils.initCSNameByTimestamp( wsName,
                ScmShardingType.YEAR, fileACreate_time );
        expCleanCSNames.remove( fileBLobCS );
        Assert.assertEqualsNoOrder( taskExtraInfo, expCleanCSNames.toArray(),
                "act:" + Arrays.toString( taskExtraInfo ) + ",exp:"
                        + expCleanCSNames );

        // 所有表都不是空表
        taskID = ScmSystem.Task.startSpaceRecyclingTask( conf );
        ScmTaskUtils.waitTaskFinish( sessionM, taskID );
        task = ScmSystem.Task.getTask( sessionM, taskID );
        Assert.assertNull( task.getExtraInfo() );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmWorkspaceUtil.deleteWs( wsName, sessionM );
            } finally {
                sessionM.close();
            }
        }
    }

    // 构造空的cs
    private void createNullCS() throws Exception {
        Calendar instance = Calendar.getInstance();
        for ( int i = 0; i < recycleCSNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( wsM );
            file.setCreateTime( instance.getTime() );
            file.setAuthor( fileAuthor );
            file.setFileName( fileName + i );
            file.setContent( filePath );
            file.save();
            instance.add( Calendar.YEAR, -1 );
        }
        ScmFileUtils.cleanFile( wsName, queryCond );
    }

    private void createFile( long fileACreate_time ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( wsM );
        file.setFileName( fileName );
        file.setAuthor( fileAuthor );
        file.setContent( filePath );
        file.setCreateTime( new Date( fileACreate_time ) );
        file.save();
    }
}