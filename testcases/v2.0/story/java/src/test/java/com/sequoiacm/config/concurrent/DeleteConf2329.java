package com.sequoiacm.config.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.BasicBSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmSchedule;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.ScmScheduleCleanFileContent;
import com.sequoiacm.client.element.ScmScheduleContent;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestThreadBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ConfUtil;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;

/**
 * @Description: SCM-2329 ::  并发删除配置和操作异步调度任务
 * @author fanyu
 * @Date:2018年12月04日
 * @version:1.0
 */
public class DeleteConf2329 extends TestScmBase {
    private String serviceName = "schedule-server";
    private SiteWrapper site = null;
    private List< ScmServiceInstance > scheList = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        scheList = ScmSystem.ServiceCenter
                .getServiceInstanceList( session, serviceName );
        ConfUtil.deleteAuditConf( serviceName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        Delete dThraed = new Delete();
        CRDScheule cThread = new CRDScheule();
        dThraed.start( 2 );
        cThread.start();
        Assert.assertEquals( dThraed.isSuccess(), true, dThraed.getErrorMsg() );
        Assert.assertEquals( cThread.isSuccess(), true, cThread.getErrorMsg() );

        //check local configuration
        List< String > list = new ArrayList< String >();
        list.add( ConfigCommonDefind.scm_audit_mask );
        list.add( ConfigCommonDefind.scm_audit_userMask );
        for ( ScmServiceInstance instance : scheList ) {
            ConfUtil.checkDeletedConf(
                    instance.getIp() + ":" + instance.getPort(), list );
        }
        //check configuration do not take effect
        ConfUtil.checkNotTakeEffect( serviceName );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf( serviceName );
        if ( session != null ) {
            session.close();
        }
    }

    private class Delete extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmUpdateConfResultSet actResult = null;
            try {
                session = TestScmTools.createSession( site );
                ScmConfigProperties confProp = ScmConfigProperties.builder()
                        .service( serviceName )
                        .deleteProperty( ConfigCommonDefind.scm_audit_mask )
                        .deleteProperty( ConfigCommonDefind.scm_audit_userMask )
                        .build();
                actResult = ScmSystem.Configuration
                        .setConfigProperties( session, confProp );

                List< String > list = new ArrayList< String >();
                list.add( ConfigCommonDefind.scm_audit_mask );
                list.add( ConfigCommonDefind.scm_audit_userMask );
                for ( ScmServiceInstance instance : scheList ) {
                    ConfUtil.checkDeletedConf(
                            instance.getIp() + ":" + instance.getPort(), list );
                }
            } catch ( ScmException e ) {
                e.printStackTrace();
                if ( actResult != null ) {
                    Assert.fail( "delete conf failed, actResult = " +
                            actResult.toString() );
                }
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }

    private class CRDScheule extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            WsWrapper wsp = ScmInfo.getWs();
            List< SiteWrapper > sites = ScmNetUtils.getCleanSites( wsp );
            SiteWrapper branSite = sites.get( 1 );
            ScmSession session = null;
            String scheName =
                    TestTools.getClassName() + "_" + UUID.randomUUID();
            ScmId scheduleId = null;
            try {
                session = TestScmTools.createSession( branSite );
                ScmScheduleContent content = new ScmScheduleCleanFileContent(
                        branSite.getSiteName(), "0d", new BasicBSONObject() );
                String cron = "* * * * * ? 2022";
                ScmSchedule sche = ScmSystem.Schedule
                        .create( session, wsp.getName(),
                                ScheduleType.CLEAN_FILE, scheName, scheName,
                                content, cron );
                scheduleId = sche.getId();
                if ( scheduleId != null ) {
                    ScmSchedule sch = ScmSystem.Schedule
                            .get( session, scheduleId );
                    Assert.assertEquals( sch.getId(), scheduleId );
                }
            } finally {
                if ( scheduleId != null ) {
                    try {
                        ScmSystem.Schedule.delete( session, scheduleId );
                    } catch ( ScmException e ) {
                        e.printStackTrace();
                        Assert.fail( e.getMessage() );
                    }
                }
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}

