package com.sequoiacm.session;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.SessionType;
import com.sequoiacm.client.core.ScmConfigOption;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @FileName SCM-317: 鉴权登入，不指定conf
 * @Author linsuqiang
 * @Date 2017-05-25
 * @Version 1.00
 */

/*
 * 1、调用session接口，指定sessionType为鉴权，但不设置用户、密码登入 2、检查执行结果正确性；
 */

public class AuthLogin317 extends TestScmBase {
    private static SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        site = ScmInfo.getSite();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        try {
            try {
                ScmConfigOption scOpt = new ScmConfigOption();
                ScmFactory.Session.createSession( SessionType.AUTH_SESSION,
                        scOpt );
                Assert.fail( "login success with invalid parameter" );
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT
                        .getErrorCode() ) {
                    throw e;
                }
            }

            try {
                ScmConfigOption scOpt2 = new ScmConfigOption(
                        TestScmBase.gateWayList.get( 0 ) + "/" + site );
                ScmFactory.Session.createSession( SessionType.AUTH_SESSION,
                        scOpt2 );
                Assert.fail( "login success with invalid parameter" );
            } catch ( ScmException e ) {
                if ( e.getErrorCode() != ScmError.INVALID_ARGUMENT
                        .getErrorCode() ) {
                    throw e;
                }
            }

        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
    }

}
