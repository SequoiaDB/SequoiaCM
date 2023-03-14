package com.sequoiacm.scheduletask.serial;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmSpaceRecycleScope;
import com.sequoiacm.client.element.ScmSpaceRecyclingTaskConfig;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.element.bizconf.ScmWorkspaceConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Descreption SCM-5244:创建空间回收任务，被回收的工作区分区方式覆盖年、季、月
 * @Author YiPan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class SpaceRecycle5244 extends TestScmBase {
    private String fileName = "file5244_";
    private String fileAuthor = "author5244";
    private String wsName = "ws_5244";
    private SiteWrapper rootSite = null;
    private ScmSession session = null;
    private ScmWorkspace ws;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private int recycleCSNum = 5;
    private AtomicInteger successTestCount = new AtomicInteger( 0 );

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
        session = ScmSessionUtils.createSession( rootSite );
    }

    @DataProvider(name = "dataProvide")
    public Object[] scmShardingTypes() {
        return new ScmShardingType[] { ScmShardingType.MONTH,
                ScmShardingType.QUARTER, ScmShardingType.YEAR };
    }

    @Test(groups = { "oneSite", "twoSite",
            "fourSite" }, dataProvider = "dataProvide")
    public void test( ScmShardingType shardingType ) throws Exception {
        // 创建ws
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        conf.setDataLocations( ScmWorkspaceUtil
                .getDataLocationList( ScmInfo.getSiteNum(), shardingType ) );
        conf.setMetaLocation(
                ScmWorkspaceUtil.getMetaLocation( shardingType ) );
        conf.setName( wsName );
        ws = ScmFactory.Workspace.createWorkspace( session, conf );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        // 构造空表
        createNullCS();

        // 启动任务
        ScmSpaceRecyclingTaskConfig taskConfig = new ScmSpaceRecyclingTaskConfig();
        taskConfig.setWorkspace( ws );
        taskConfig.setRecycleScope( ScmSpaceRecycleScope.mothBefore( 0 ) );
        ScmId taskID = ScmSystem.Task.startSpaceRecyclingTask( taskConfig );
        ScmTaskUtils.waitTaskFinish( session, taskID );

        // 校验结果
        Set< String > expCleanCSNames = ScmScheduleUtils.initLobCSName( wsName,
                shardingType, System.currentTimeMillis(), recycleCSNum );
        ScmTask task = ScmSystem.Task.getTask( session, taskID );
        Object[] taskExtraInfo = ScmScheduleUtils.getTaskExtraInfo( task );
        Assert.assertEqualsNoOrder( taskExtraInfo, expCleanCSNames.toArray(),
                "act:" + Arrays.toString( taskExtraInfo ) + ",exp:"
                        + expCleanCSNames );
        successTestCount.getAndIncrement();
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( successTestCount.get() == scmShardingTypes().length
                || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmWorkspaceUtil.deleteWs( wsName, session );
            } finally {
                session.close();
            }
        }
    }

    // 构造空的cs
    private void createNullCS() throws ScmException, InterruptedException {
        Calendar instance = Calendar.getInstance();
        ScmScheduleUtils.checkMonthChange( instance );
        for ( int i = 0; i < recycleCSNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setCreateTime( instance.getTime() );
            file.setAuthor( fileAuthor );
            file.setFileName( fileName + i );
            ScmId fileId = file.save();
            ScmFactory.File.deleteInstance( ws, fileId, true );
            instance.add( Calendar.YEAR, -1 );
        }
    }
}