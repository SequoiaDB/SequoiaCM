package com.sequoiacm.session;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @descreption SCM-5560:创建驱动池，驱动测试
 * @author YiPan
 * @date 2022/12/20
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr5560 extends TestScmBase {
    private ScmSessionMgr sessionMgr;
    private ScmSession session;
    private final String nodeGroup = "test5560";

    @BeforeClass
    private void setUp() {
    }

    @Test
    private void test() throws ScmException {
        // 不指定网关分组和模式
        ScmSessionPoolConf conf = createSessionPoolConf( getDefaultUrl(),
                TestScmBase.scmUserName, TestScmBase.scmPassword );
        Assert.assertNull( conf.getNodeGroup() );
        Assert.assertNull( conf.getGroupAccessMode() );
        sessionMgr = ScmFactory.Session.createSessionMgr( conf );
        session = sessionMgr.getSession();
        Assert.assertNotNull( session );

        // 清理
        session.close();
        sessionMgr.close();

        // 指定分组和模式
        conf.setNodeGroup( nodeGroup );
        conf.setGroupAccessMode( ScmType.NodeGroupAccessMode.ACROSS );
        Assert.assertEquals( conf.getNodeGroup(), nodeGroup );
        Assert.assertEquals( conf.getGroupAccessMode(),
                ScmType.NodeGroupAccessMode.ACROSS );
        sessionMgr = ScmFactory.Session.createSessionMgr( conf );
        session = sessionMgr.getSession();
        Assert.assertNotNull( session );
    }

    @AfterClass
    private void tearDown() {
        if ( session != null ) {
            session.close();
        }
        if ( sessionMgr != null ) {
            sessionMgr.close();
        }
    }

    private ScmSessionPoolConf createSessionPoolConf( String url,
            String username, String password ) throws ScmException {
        List< String > urlList = new ArrayList<>();
        urlList.add( url );
        ScmConfigOption scmConfigOption = new ScmConfigOption( urlList,
                username, password );
        return ScmSessionPoolConf.builder().setSessionConfig( scmConfigOption )
                .get();
    }

    private String getDefaultUrl() {
        return TestScmBase.gateWayList.get( 0 ) + "/"
                + ScmInfo.getSite().getSiteName();
    }
}
