package com.sequoiacm.config;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.element.ScmUpdateConfResultSet;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.scmutils.ConfUtil;

/**
 * @Description: SCM-2320 :: 指定服务为content-server、authserver、schedule,删除配置项
 * @author fanyu
 * @Date:2018年12月13日
 * @version:1.0
 */
public class DeleteConf2320 extends TestScmBase {
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, InterruptedException {
        site = ScmInfo.getRootSite();
        ConfUtil.deleteAuditConf( "schedule-server" );
        ConfUtil.deleteAuditConf( "auth-server" );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        ScmSession session = null;
        try {
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .service( "schedule-server", "auth-server" )
                    .deleteProperty( ConfigCommonDefind.scm_audit_mask )
                    .deleteProperty( ConfigCommonDefind.scm_audit_userMask )
                    .build();
            session = TestScmTools.createSession( site );
            ScmUpdateConfResultSet actResult = ScmSystem.Configuration
                    .setConfigProperties( session, confProp );

            List< ScmServiceInstance > scheList = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session, "schedule-server" );
            List< ScmServiceInstance > authList = ScmSystem.ServiceCenter
                    .getServiceInstanceList( session, "auth-server" );

            Assert.assertTrue( actResult.getFailures().size() == 0,
                    actResult.getFailures().toString() );
            Assert.assertTrue( actResult.getSuccesses().size() ==
                            ( scheList.size() + authList.size() ),
                    actResult.getSuccesses().toString() );

            List< String > expKeys = new ArrayList< String >();
            expKeys.add( ConfigCommonDefind.scm_audit_mask );
            expKeys.add( ConfigCommonDefind.scm_audit_userMask );

            for ( ScmServiceInstance instance : scheList ) {
                ConfUtil.checkDeletedConf(
                        instance.getIp() + ":" + instance.getPort(), expKeys );
            }

            for ( ScmServiceInstance instance : authList ) {
                ConfUtil.checkDeletedConf(
                        instance.getIp() + ":" + instance.getPort(), expKeys );
            }
            ConfUtil.checkNotTakeEffect( "schedule-server" );
            ConfUtil.checkNotTakeEffect( "auth-server" );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException, InterruptedException {
        ConfUtil.deleteAuditConf( "schedule-server" );
        ConfUtil.deleteAuditConf( "auth-server" );
    }
}