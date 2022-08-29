package com.sequoiacm.site;

import java.util.ArrayList;
import java.util.List;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmServiceInstance;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.NodeWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @Description: SCM-2587:查询注册中心内容服务节点
 * @author fanyu
 * @Date:2019年09月03日
 * @version:1.0
 */
public class ListContentServer2587 extends TestScmBase {
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        site = ScmInfo.getSite();
    }

    @Test(groups = { GroupTags.base })
    private void testListNodes() throws Exception {
        List< NodeWrapper > nodes = ScmInfo.getAllNodes();
        List< Integer > ports = new ArrayList<>();
        for ( NodeWrapper node : nodes ) {
            ports.add( node.getPort() );
        }
        ScmSession session = null;
        List< ScmServiceInstance > contentServers = null;
        try {
            session = TestScmTools.createSession( site );
            contentServers = ScmSystem.ServiceCenter
                    .getContentServerInstanceList( session );
            Assert.assertEquals( contentServers.size(), nodes.size() );
            // 内容无法进行精确的校验，只能判断不为空
            for ( ScmServiceInstance scmServiceInstance : contentServers ) {
                Assert.assertEquals(
                        ports.contains( scmServiceInstance.getPort() ), true );
                Assert.assertNotNull( scmServiceInstance.getIp() );
                Assert.assertNotNull( scmServiceInstance.getRegion() );
                Assert.assertNotNull( scmServiceInstance.getServiceName() );
                Assert.assertEquals( scmServiceInstance.getStatus(), "UP" );
                Assert.assertNotNull( scmServiceInstance.getZone() );
            }
        } catch ( AssertionError e ) {
            throw new Exception( "contentServers = " + contentServers.toString()
                    + ",nodes = " + nodes.toString(), e );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @Test(groups = { GroupTags.base })
    private void testSessionIsNull() throws Exception {
        try {
            ScmSystem.ServiceCenter.getContentServerInstanceList( null );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                throw e;
            }
        }
    }

    @Test(groups = { GroupTags.base })
    private void testSessionIsClosed() throws Exception {
        ScmSession session = TestScmTools.createSession( site );
        session.close();
        try {
            ScmSystem.ServiceCenter.getContentServerInstanceList( session );
            Assert.fail( "exp fail but act success!!!" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.SESSION_CLOSED ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
    }
}
