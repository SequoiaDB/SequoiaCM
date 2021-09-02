package com.sequoiacm.scheduletask;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmScheduleBasicInfo;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @description SCM-1228:创建调度任务，类型为清理，指定站点为主站点
 * @author huangxiaoni
 * @createDate 2018-04-17
 * @updateUser YiPan
 * @updateDate 2021.9.2
 * @updateRemark 星状主站点不能创建清理任务约束已取消，适配用例
 * @version 1.1
 */

public class CreateSche_cleanRootSiteFile1228 extends TestScmBase {
    private final static String name = "schetask1228";
    private SiteWrapper rootSite = null;
    private WsWrapper wsp = null;
    private ScmSession session;
    private ScmSchedule scmSchedule;
    private boolean runSuccess = false;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( rootSite );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        String maxStayTime = "0d";
        BSONObject queryCond = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).is( name ).get();
        ScmScheduleCleanFileContent content = new ScmScheduleCleanFileContent(
                rootSite.getSiteName(), maxStayTime, queryCond );
        String cron = "* * * * * ?";
        scmSchedule = ScmSystem.Schedule.create( session, wsp.getName(),
                ScheduleType.CLEAN_FILE, name, "", content, cron );

        BSONObject object = ScmQueryBuilder
                .start( ScmAttributeName.Schedule.ID )
                .is( scmSchedule.getId().get() ).get();
        ScmCursor< ScmScheduleBasicInfo > cursor = ScmSystem.Schedule
                .list( session, object );
        Assert.assertTrue( cursor.hasNext() );
        cursor.close();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                ScmSystem.Schedule.delete( session, scmSchedule.getId() );
                ScmScheduleUtils.cleanTask( session, scmSchedule.getId() );
            } finally {
                session.close();
            }
        }
    }

}