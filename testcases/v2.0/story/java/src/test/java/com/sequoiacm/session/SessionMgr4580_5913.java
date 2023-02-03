package com.sequoiacm.session;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4580:用户创建session池，指定网关同步周期参数
 *              SCM-5913:ScmConfigOption对象默认region和zone验证
 * @author YiPan
 * @date 2022/7/19
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class SessionMgr4580_5913 extends TestScmBase {
    private ScmConfigOption scmConfigOption;
    private ScmSessionPoolConf conf;
    private String defaultRegion = "DefaultRegion";
    private String defaultZone = "zone1";
    private List< ScmSession > sessions = new ArrayList<>();

    @BeforeClass
    private void setUp() throws ScmException {
        scmConfigOption = TestScmTools
                .getScmConfigOption( ScmInfo.getRootSite().getSiteName() );
        conf = ScmSessionPoolConf.builder().get();
        conf.setSessionConfig( scmConfigOption );
    }

    @Test
    private void test() throws ScmException {
        // default=0
        checkSynGatewayUrlsInterval( conf, 0 );

        // 校验大于1000值被设置正确
        conf.setSynGatewayUrlsInterval( 2000 );
        checkSynGatewayUrlsInterval( conf, 2000 );

        // 校验小于1000大于0的值被修正为1000
        conf.setSynGatewayUrlsInterval( 100 );
        checkSynGatewayUrlsInterval( conf, 1000 );

        // 校验小于0值被设置正确(实际按0生效)
        conf.setSynGatewayUrlsInterval( -100 );
        checkSynGatewayUrlsInterval( conf, -100 );

        // 校验默认region和zone
        Assert.assertEquals( scmConfigOption.getRegion(), defaultRegion );
        Assert.assertEquals( scmConfigOption.getZone(), defaultZone );
    }

    @AfterClass
    private void tearDown() {
        for ( ScmSession session : sessions ) {
            session.close();
        }
    }

    private void checkSynGatewayUrlsInterval( ScmSessionPoolConf conf, int exp )
            throws ScmException {
        ScmSessionMgr sessionMgr = null;
        try {
            sessionMgr = ScmFactory.Session.createSessionMgr( conf );
            Assert.assertEquals( conf.getSynGatewayUrlsInterval(), exp );
            sessions.add( sessionMgr.getSession() );
        } finally {
            if ( sessionMgr != null ) {
                sessionMgr.close();
            }
        }
    }
}
