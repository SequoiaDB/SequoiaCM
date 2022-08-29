package com.sequoiacm.session.seria;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ServerScope;
import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmSystem;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-318: 不鉴权登入，指定conf包含正确的用户/密码登入
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、调用session接口，指定sessionType为非鉴权，并设置用户、密码登入 2、登入后分别做业务操作（如写文件）和非业务操作（如刷新业务配置）；
 * 3、检查登入结果正确性；检查业务操作和非业务操作结果正确性；
 */

public class NotAuthLogin318 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
    }

    @Test(groups = { GroupTags.base })
    private void test() {
        ScmSession session = null;
        try {
            String user = TestScmBase.scmUserName;
            String passwd = TestScmBase.scmPassword;
            ScmConfigOption scOpt = new ScmConfigOption(
                    TestScmBase.gateWayList.get( 0 ) + "/"
                            + site.getSiteServiceName(),
                    user, passwd );
            session = ScmFactory.Session
                    .createSession( SessionType.NOT_AUTH_SESSION, scOpt );
            ScmSystem.Configuration.reloadBizConf( ServerScope.SITE,
                    site.getSiteId(), session );
            try {
                ScmWorkspace ws = ScmFactory.Workspace
                        .getWorkspace( wsp.getName(), session );
                ScmFactory.File.createInstance( ws );
                Assert.fail(
                        "business operation shouldn't succeed when login is "
                                + "not authorized" );
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getErrorCode(),
                        ScmError.HTTP_FORBIDDEN.getErrorCode(),
                        e.getMessage() );
            }

        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
    }

}
