package com.sequoiacm.scheduletask;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1227:创建调度任务，类型为迁移，指定源和目标站点错误
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @updateUser YiPan
 * @updateDate 2021.9.2
 * @updateRemark 星状主站点不能迁移文件至分站点任务约束已取消，适配用例
 * @version 1.1
 */

public class CreateSche_siteError1227 extends TestScmBase {
    private final static int branSiteNum = 2;
    private final static String name = "schetask1227";
    private final static String cron = "* * * * * ?";
    private final static String maxStayTime = "0d";
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private BSONObject queryCond = null;
    private boolean runSuccess = false;
    private List< ScmId > scheduleIds = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        branSites = ScmInfo.getBranchSites( branSiteNum );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( rootSite );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        // 主站点迁移到分站点
        ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
                rootSite.getSiteName(), branSites.get( 0 ).getSiteName(),
                maxStayTime, queryCond );
        ScmSchedule scmSchedule = ScmSystem.Schedule.create( session,
                wsp.getName(), ScheduleType.COPY_FILE, name, "", content,
                cron );
        scheduleIds.add( scmSchedule.getId() );

        // 源站点和目标站点都为主站点
        try {
            content = new ScmScheduleCopyFileContent( rootSite.getSiteName(),
                    rootSite.getSiteName(), maxStayTime, queryCond );
            ScmSystem.Schedule.create( session, wsp.getName(),
                    ScheduleType.COPY_FILE, name, "", content, cron );
            Assert.fail( "expect fail but actual success" );
        } catch ( ScmException e ) {
            if ( ScmError.HTTP_BAD_REQUEST != e.getError() ) {
                throw e;
            }
        }

        try {
            content = new ScmScheduleCopyFileContent(
                    branSites.get( 0 ).getSiteName(),
                    branSites.get( 1 ).getSiteName(), maxStayTime, queryCond );
            ScmSystem.Schedule.create( session, wsp.getName(),
                    ScheduleType.COPY_FILE, name, "", content, cron );
            Assert.fail( "expect fail but actual success" );
        } catch ( ScmException e ) {
            if ( ScmError.HTTP_BAD_REQUEST != e.getError() ) {
                throw e;
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                for ( ScmId id : scheduleIds ) {
                    ScmSystem.Schedule.delete( session, id );
                    ScmScheduleUtils.cleanTask( session, id );
                }
            } finally {
                session.close();
            }
        }
    }

}