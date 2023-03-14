package com.sequoiacm.config.concurrent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.sequoiacm.client.element.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.config.ConfigCommonDefind;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @author fanyu
 * @Description: SCM-2302:并发修改配置和操作异步调度任务
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateConf2302 extends TestScmBase {
    private String serviceName = "schedule-server";
    private SiteWrapper site = null;
    private ScmSession session = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ConfUtil.deleteAuditConf( serviceName );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        Update uThraed = new Update();
        CRDScheule cThread = new CRDScheule();
        uThraed.start( 3 );
        cThread.start();
        Assert.assertEquals( uThraed.isSuccess(), true, uThraed.getErrorMsg() );
        Assert.assertEquals( cThread.isSuccess(), true, cThread.getErrorMsg() );

        // check local configuration
        List< ScmServiceInstance > instancesList = ScmSystem.ServiceCenter
                .getServiceInstanceList( session, serviceName );
        Map< String, String > map = new HashMap< String, String >();
        map.put( ConfigCommonDefind.scm_audit_mask, "ALL" );
        map.put( ConfigCommonDefind.scm_audit_userMask, "LOCAL" );
        for ( ScmServiceInstance instances : instancesList ) {
            ConfUtil.checkUpdatedConf(
                    instances.getIp() + ":" + instances.getPort(), map );
        }
        // check updated configuration take effect
        ConfUtil.checkTakeEffect( serviceName );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf( serviceName );
        if ( session != null ) {
            session.close();
        }
    }

    private class Update extends TestThreadBase {
        @Override
        public void exec() throws Exception {
            ScmSession session = null;
            ScmUpdateConfResultSet results = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmConfigProperties confProp = ScmConfigProperties.builder()
                        .service( serviceName )
                        .updateProperty( ConfigCommonDefind.scm_audit_mask,
                                "ALL" )
                        .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                                "LOCAL" )
                        .build();
                results = ScmSystem.Configuration.setConfigProperties( session,
                        confProp );
                System.out.println( "result = " + results.toString() );
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
            crdSche();
        }

        private void crdSche() throws ScmException {
            WsWrapper wsp = ScmInfo.getWs();
            SiteWrapper branSite = ScmInfo.getSite();
            ScmSession session = null;
            String scheName = TestTools.getClassName() + "_"
                    + UUID.randomUUID();
            ScmId scheduleId = null;
            try {
                session = ScmSessionUtils.createSession( branSite );
                ScmScheduleContent content = new ScmScheduleCleanFileContent(
                        branSite.getSiteName(), "0d", new BasicBSONObject() );
                String cron = "* * * * * ?";
                ScmSchedule sche = ScmSystem.Schedule.create( session,
                        wsp.getName(), ScheduleType.CLEAN_FILE, scheName,
                        scheName, content, cron );
                scheduleId = sche.getId();
                ScmSchedule sch = ScmSystem.Schedule.get( session, scheduleId );
                Assert.assertEquals( sch.getId(), scheduleId );
            } finally {
                if ( scheduleId != null ) {
                    ScmSystem.Schedule.delete( session, scheduleId );
                }
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
