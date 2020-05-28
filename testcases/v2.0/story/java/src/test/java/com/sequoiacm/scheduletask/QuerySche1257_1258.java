package com.sequoiacm.scheduletask;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleBasicInfo;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmScheduleUtils;

/**
 * @FileName SCM-1257:获取调度任务列表，指定过滤条件覆盖所有任务属性及匹配符
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class QuerySche1257_1258 extends TestScmBase {
    private final static String name = "schetask1257";
    private final static String desc = "desc";
    private final static ScheduleType type = ScheduleType.CLEAN_FILE;
    private final static String cron = "* * * * * ?";
    private final static String maxStayTime = "0d";
    private final static int scheNum = 2;
    private boolean runSuccess = false;
    private int failTimes = 0;
    private ScmSession ssA = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private WsWrapper wsp = null;
    private BSONObject queryCond = null;
    private ScmScheduleCleanFileContent content = null;
    private List< ScmId > scheIds = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        rootSite = ScmInfo.getRootSite();
        branSite = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();
        ssA = TestScmTools.createSession( branSite );

        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( name ).get();

        this.createSchedules();
    }

    @BeforeMethod
    private void initMethod() {
        if ( !runSuccess ) {
            failTimes++;
        }
        runSuccess = false;
    }

    @AfterMethod
    private void afterMethod() {
        if ( failTimes > 1 ) {
            runSuccess = false;
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test_baseCase() throws Exception {
        ScmCursor< ScmScheduleBasicInfo > cursor = null;
        try {
            BSONObject cond = ScmQueryBuilder.start( "name" ).is( name ).get();
            cursor = ScmSystem.Schedule.list( ssA, cond );
            int actNum = 0;
            while ( cursor.hasNext() ) {
                ScmScheduleBasicInfo info = cursor.getNext();
                Assert.assertEquals( info.getName(), name );
                Assert.assertEquals( info.getDesc(), desc );
                Assert.assertEquals( info.getType(), type );
                Assert.assertEquals( info.getCron(), cron );
                Assert.assertEquals( info.getWorkspace(), wsp.getName() );
                Assert.assertNotNull( info.getId() );

                actNum++;
            }
            Assert.assertEquals( actNum, scheNum - 1 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            throw e;
        } finally {
            if ( null != cursor ) {
                cursor.close();
            }
        }
        runSuccess = true;
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test_coverAllTaskAttr() throws Exception {
        ScmCursor< ScmScheduleBasicInfo > cursor = null;
        try {
            BSONObject queryCond2 = ScmQueryBuilder
                    .start( ScmAttributeName.File.AUTHOR ).in( name ).get();
            ScmScheduleCopyFileContent content2 = new ScmScheduleCopyFileContent(
                    branSite.getSiteName(), rootSite.getSiteName(), "1d",
                    queryCond2 );

            BSONObject sourceSite = ScmQueryBuilder.start( "source_site" )
                    .is( branSite.getSiteName() ).get();
            BSONObject targetSite = ScmQueryBuilder.start( "target_site" )
                    .is( rootSite.getSiteName() ).get();
            BSONObject mstObj = ScmQueryBuilder.start( "max_stay_time" )
                    .is( "1d" ).get();
            BSONObject type2 = ScmQueryBuilder.start( "type" )
                    .is( "clean_file" ).get();
            BSONObject ctObj = ScmQueryBuilder.start( "content" )
                    .is( content2.toBSONObject() ).get();

            BSONObject cond = ScmQueryBuilder.start().and( "name" )
                    .is( name + "_new" ).and( "desc" ).in( desc, desc + "_new" )
                    .and( "workspace" ).exists( wsp.getName() ).and( "cron" )
                    .is( "1 * * * * ?" ).and( "content" )
                    .elemMatch( sourceSite ).and( "content" )
                    .elemMatch( targetSite ).and( "content" )
                    .elemMatch( mstObj ).not( type2 ).or( ctObj ).get();
            // System.out.println(cond);
            cursor = ScmSystem.Schedule.list( ssA, cond );
            int actNum = 0;
            while ( cursor.hasNext() ) {
                cursor.getNext();
                actNum++;
            }
            Assert.assertEquals( actNum, 1 );
        } catch ( ScmException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( null != cursor ) {
                cursor.close();
            }
        }
        runSuccess = true;
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test_returnEmpty() throws Exception {
        BSONObject cond = ScmQueryBuilder.start( "name" ).is( "123" ).get();
        ScmCursor< ScmScheduleBasicInfo > cursor = ScmSystem.Schedule.list( ssA,
                cond );
        Assert.assertFalse( cursor.hasNext() );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            for ( ScmId scheduleId : scheIds ) {
                ScmSystem.Schedule.delete( ssA, scheduleId );
                if ( runSuccess || TestScmBase.forceClear ) {
                    ScmScheduleUtils.cleanTask( ssA, scheduleId );
                }
            }
        } finally {
            if ( ssA != null ) {
                ssA.close();
            }
        }
    }

    private void createSchedules() throws ScmException {
        content = new ScmScheduleCleanFileContent( branSite.getSiteName(),
                maxStayTime, queryCond );
        for ( int i = 0; i < scheNum - 1; i++ ) {
            ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsp.getName(),
                    type, name, desc, content, cron );
            ScmId scheduleId = sche.getId();
            scheIds.add( scheduleId );
        }

        ScheduleType taskType2 = ScheduleType.COPY_FILE;
        String taskName = name + "_new";
        String cron2 = "1 * * * * ?";
        String maxStayTime2 = "1d";
        String desc2 = desc + "_new";
        BSONObject queryCond2 = ScmQueryBuilder
                .start( ScmAttributeName.File.AUTHOR ).in( name ).get();
        ScmScheduleCopyFileContent content2 = new ScmScheduleCopyFileContent(
                branSite.getSiteName(), rootSite.getSiteName(), maxStayTime2,
                queryCond2 );
        ScmSchedule sche = ScmSystem.Schedule.create( ssA, wsp.getName(),
                taskType2, taskName, desc2, content2, cron2 );
        ScmId scheduleId = sche.getId();
        ScmSystem.Schedule.get( ssA, scheduleId );
        scheIds.add( scheduleId );
    }
}