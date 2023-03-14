package com.sequoiacm.scheduletask;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmScheduleMoveFileContent;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import com.sequoiacm.testcommon.scmutils.ScmTaskUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @Descreption SCM-5214:创建迁移并清理调度任务，源站点不存在文件
 * @Author Yipan
 * @CreateDate 2022/9/21
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class MoveFile5214 extends TestScmBase {
    private String fileName = "file5214";
    private String taskName = "task5214";
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private ScmSession sessionM = null;
    private WsWrapper wsp;
    private BSONObject queryCond;
    private ScmSchedule sche;
    private int fileSize = 1024 * 100;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        wsp = ScmInfo.getWs();
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        sessionM = ScmSessionUtils.createSession( rootSite );
        // 匹配空文件
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( fileName ).get();
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws Exception {
        // 创建迁移并清理任务
        ScmScheduleMoveFileContent content = new ScmScheduleMoveFileContent(
                rootSite.getSiteName(), branchSite.getSiteName(), "0d",
                queryCond, ScmType.ScopeType.SCOPE_CURRENT );

        // 启动迁移并清理调度任务
        String cron = "0/1 * * * * ?";
        sche = ScmSystem.Schedule.create( sessionM, wsp.getName(),
                ScheduleType.MOVE_FILE, taskName, "", content, cron );

        // 校验任务执行结果
        ScmScheduleUtils.waitForTask( sche, 3 );
        sche.disable();

        // 校验统计记录
        List< ScmTask > tasks = sche.getTasks( null, null, 0, -1 );
        for ( int i = 0; i < tasks.size(); i++ ) {
            ScmTask task = tasks.get( i );
            ScmTaskUtils.waitTaskFinish( sessionM, task.getId() );
            Assert.assertEquals( task.getEstimateCount(), 0 );
            Assert.assertEquals( task.getActualCount(), 0 );
            Assert.assertEquals( task.getSuccessCount(), 0 );
            Assert.assertEquals( task.getFailCount(), 0 );
            Assert.assertEquals( task.getProgress(), 100 );
        }
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                TestTools.LocalFile.removeFile( localPath );
                ScmSystem.Schedule.delete( sessionM, sche.getId() );
                ScmScheduleUtils.cleanTask( sessionM, sche.getId() );
            } finally {
                sessionM.close();
            }
        }
    }
}