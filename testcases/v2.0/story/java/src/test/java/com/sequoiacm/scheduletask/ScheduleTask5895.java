package com.sequoiacm.scheduletask;

import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmTask;
import com.sequoiacm.testcommon.*;

/**
 * @descreption SCM-5895:创建调度任务，配置执行时间已经过期的cron
 * @author YiPan
 * @date 2023/2/2
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScheduleTask5895 extends TestScmBase {
    private final static String filename = "file5895";
    private final static String cron = "* * * * * ? 2010";
    private ScmSession session = null;
    private SiteWrapper rootSite = null;
    private WsWrapper wsp = null;
    private BSONObject queryCond = null;
    private ScmSchedule sche;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( rootSite );
        queryCond = ScmQueryBuilder.start( ScmAttributeName.File.FILE_NAME )
                .is( filename ).get();
    }

    @Test
    public void test() throws Exception {
        // 创建调度任务
        ScmScheduleContent content = new ScmScheduleCleanFileContent(
                rootSite.getSiteName(), "0d", queryCond );
        sche = ScmSystem.Schedule.create( session, wsp.getName(),
                ScheduleType.CLEAN_FILE, "sch5895", "test", content, cron );
        // 默认为开启
        Assert.assertTrue( sche.isEnable() );

        // 获取实际结果校验为禁用
        Thread.sleep( 1500 );
        ScmSchedule getSche = ScmSystem.Schedule.get( session, sche.getId() );
        Assert.assertFalse( getSche.isEnable() );
        List< ScmTask > tasks = getSche.getTasks( null, null, 0, 1 );
        Assert.assertEquals( tasks.size(), 0, tasks.toString() );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                ScmSystem.Schedule.delete( session, sche.getId() );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

}