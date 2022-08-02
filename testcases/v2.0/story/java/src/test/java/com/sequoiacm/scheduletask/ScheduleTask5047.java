package com.sequoiacm.scheduletask;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.bson.BSONObject;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @descreption SCM-5047:不同工作区，创建、重命名出重名调度任务
 * @author YiPan
 * @date 2022/8/2
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScheduleTask5047 extends TestScmBase {
    private final static String taskNameA = "scheduleTask5047A";
    private final static String taskNameB = "scheduleTask5047B";
    private final static String authorName = "author5047";
    private List< ScmId > taskIds = new ArrayList<>();
    private boolean runSuccess = false;
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private WsWrapper wsp1 = null;
    private WsWrapper wsp2 = null;
    private ScmScheduleCopyFileContent content = null;
    private String cron = "* * * * * ?";

    @BeforeClass
    private void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        List< WsWrapper > wsList = ScmInfo.getWss( 2 );
        wsp1 = wsList.get( 0 );
        wsp2 = wsList.get( 1 );
        session = TestScmTools.createSession( rootSite );
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( authorName ).get();
        String maxStayTime = "0d";
        content = new ScmScheduleCopyFileContent( branchSite.getSiteName(),
                rootSite.getSiteName(), maxStayTime, queryCond );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        // ws1创建taskNameA的任务
        ScmSchedule scmSchedule = ScmSystem.Schedule.create( session,
                wsp1.getName(), ScheduleType.COPY_FILE, taskNameA, "test",
                content, cron );
        taskIds.add( scmSchedule.getId() );

        // ws2创建重名任务成功
        scmSchedule = ScmSystem.Schedule.create( session, wsp2.getName(),
                ScheduleType.COPY_FILE, taskNameA, "test", content, cron );
        taskIds.add( scmSchedule.getId() );

        // 重命名为taskNameB
        scmSchedule.updateName( taskNameB );

        // 重命名为taskNameA
        scmSchedule.updateName( taskNameA );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId taskId : taskIds ) {
                    ScmSystem.Schedule.delete( session, taskId );
                    ScmScheduleUtils.cleanTask( session, taskId );
                }
            }
        } finally {
            session.close();
        }
    }
}