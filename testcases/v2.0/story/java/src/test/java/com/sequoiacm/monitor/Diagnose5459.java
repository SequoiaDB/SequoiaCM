package com.sequoiacm.monitor;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmCursor;
import com.sequoiacm.client.core.ScmHealth;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.element.ScmCheckConnResult;
import com.sequoiacm.client.element.ScmCheckConnTarget;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;

/**
 * @descreption SCM-5459:ScmCheckConnTarget驱动测试
 * @author YiPan
 * @date 2023/1/4
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Diagnose5459 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String srcNode = null;
    private List< String > nodes;

    @BeforeClass
    private void setUp() throws ScmException {
        site = ScmInfo.getRootSite();
        session = TestScmTools.createSession( site );
        nodes = listNodes( session );
        if ( nodes.size() < 3 ) {
            Assert.fail( "the num of nodes less than 3," + nodes );
        }
        srcNode = nodes.get( 0 );
    }

    @Test
    private void test() throws Exception {
        // 指定所有节点
        ScmCheckConnTarget target = ScmCheckConnTarget.builder().allInstance()
                .build();
        List< ScmCheckConnResult > result = ScmSystem.Diagnose
                .checkConnectivity( srcNode, target );
        checkResult( result );

        // 指定部分节点
        target = ScmCheckConnTarget.builder()
                .instance( nodes.get( 1 ), nodes.get( 2 ) ).build();
        result = ScmSystem.Diagnose.checkConnectivity( srcNode, target );
        checkResult( result );

        // 指定不存在节点
        target = ScmCheckConnTarget.builder().instance( "abc" ).build();
        try {
            ScmSystem.Diagnose.checkConnectivity( srcNode, target );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }

        // 指定部分服务
        target = ScmCheckConnTarget.builder()
                .service( ScmInfo.getSite().getSiteServiceName() ).build();
        result = ScmSystem.Diagnose.checkConnectivity( srcNode, target );
        checkResult( result );

        // 指定不存在服务
        target = ScmCheckConnTarget.builder().service( "abc" ).build();
        try {
            ScmSystem.Diagnose.checkConnectivity( srcNode, target );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            if ( !e.getError().equals( ScmError.HTTP_INTERNAL_SERVER_ERROR ) ) {
                throw e;
            }
        }

    }

    @AfterClass
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
    }

    private void checkResult( List< ScmCheckConnResult > scmCheckConnResults ) {
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

    private List< String > listNodes( ScmSession session ) throws ScmException {
        ScmCursor< ScmHealth > scmHealthScmCursor = ScmSystem.Monitor
                .listHealth( session, null );
        List< String > nodes = new ArrayList<>();
        while ( scmHealthScmCursor.hasNext() ) {
            nodes.add( scmHealthScmCursor.getNext().getNodeName() );
        }
        scmHealthScmCursor.close();
        return nodes;
    }
}
