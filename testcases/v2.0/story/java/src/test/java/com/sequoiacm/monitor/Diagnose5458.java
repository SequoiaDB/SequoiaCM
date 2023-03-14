package com.sequoiacm.monitor;

import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmCheckConnResult;
import com.sequoiacm.client.element.ScmServiceInstance;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @descreption SCM-5458:ScmSystem.Diagnose驱动测试
 * @author YiPan
 * @date 2023/1/4
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Diagnose5458 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getRootSite();
        session = ScmSessionUtils.createSession( site );
    }

    @Test
    private void test() throws Exception {
        Map< ScmServiceInstance, List< ScmCheckConnResult > > scmServiceInstanceListMap = ScmSystem.Diagnose
                .checkConnectivity( session );

        Set< ScmServiceInstance > scmServiceInstances = scmServiceInstanceListMap
                .keySet();
        Assert.assertNotEquals( scmServiceInstances.size(), 0 );

        for ( ScmServiceInstance key : scmServiceInstances ) {
            List< ScmCheckConnResult > scmCheckConnResults = scmServiceInstanceListMap
                    .get( key );
            for ( ScmCheckConnResult result : scmCheckConnResults ) {
                Assert.assertNotNull( result.getIp() );
                Assert.assertNotNull( result.getPort() );
                Assert.assertNotNull( result.getHost() );
                Assert.assertNotNull( result.getRegion() );
                Assert.assertNotNull( result.getService() );
                Assert.assertNotNull( result.getZone() );
                Assert.assertTrue( result.isConnected() );
            }
        }
    }

    @AfterClass
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }
}
