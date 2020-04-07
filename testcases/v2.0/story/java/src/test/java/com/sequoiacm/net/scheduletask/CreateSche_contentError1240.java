package com.sequoiacm.net.scheduletask;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmScheduleCopyFileContent;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1240:创建调度任务，content缺少必填字段
 * @Author huangxiaoni
 * @Date 2018-04-17
 * @Version 1.00
 */

public class CreateSche_contentError1240 extends TestScmBase {
    private final static String name = "schetask1240";
    private final static String cron = "* * * * * ?";
    private SiteWrapper rootSite = null;
    private SiteWrapper branSite = null;
    private WsWrapper wsp = null;
    private ScmSession ss = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            wsp = ScmInfo.getWs();
            rootSite = ScmInfo.getRootSite();
            branSite = ScmInfo.getBranchSite();
            ss = TestScmTools.createSession( rootSite );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test_contentLackSrSite() throws Exception {
        try {
            BSONObject queryCond = ScmQueryBuilder.start().get();
            BSONObject cond = ScmQueryBuilder.start()
                    .and( "target_site" ).is( branSite.getSiteName() )
                    .and( "max_stay_time" ).is( "0d" )
                    .and( "extra_condition" ).is( queryCond )
                    .get();
            ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
                    cond );
            ScmSystem.Schedule.create( ss, wsp.getName(),
                    ScheduleType.COPY_FILE, name, "", content, cron );
            Assert.fail( "expect fail but actual succ." );
        } catch ( ScmException e ) {
            if ( ScmError.HTTP_BAD_REQUEST != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test_contentLackTgSite() throws Exception {
        try {
            BSONObject queryCond = ScmQueryBuilder.start().get();
            BSONObject cond = ScmQueryBuilder.start()
                    .and( "source_site" ).is( branSite.getSiteName() )
                    .and( "max_stay_time" ).is( "0d" )
                    .and( "extra_condition" ).is( queryCond )
                    .get();
            ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
                    cond );
            ScmSystem.Schedule.create( ss, wsp.getName(),
                    ScheduleType.COPY_FILE, name, "", content, cron );
            Assert.fail( "expect fail but actual succ." );
        } catch ( ScmException e ) {
            if ( ScmError.HTTP_BAD_REQUEST != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test_contentLackMST() throws Exception {
        try {
            BSONObject queryCond = ScmQueryBuilder.start().get();
            BSONObject cond = ScmQueryBuilder.start()
                    .and( "source_site" ).is( branSite.getSiteName() )
                    .and( "target_site" ).is( rootSite.getSiteName() )
                    .and( "extra_condition" ).is( queryCond )
                    .get();
            ScmScheduleCopyFileContent content = new ScmScheduleCopyFileContent(
                    cond );
            ScmSystem.Schedule.create( ss, wsp.getName(),
                    ScheduleType.COPY_FILE, name, "", content, cron );
            Assert.fail( "expect fail but actual succ." );
        } catch ( ScmException e ) {
            if ( ScmError.HTTP_BAD_REQUEST != e.getError() ) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        if ( null != ss ) {
            ss.close();
        }
    }

}