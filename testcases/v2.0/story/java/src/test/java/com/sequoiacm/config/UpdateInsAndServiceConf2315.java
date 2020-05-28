package com.sequoiacm.config;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.element.ScmConfigProperties;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @author fanyu
 * @Description: SCM-2315 ::指定动态生效类型为服务和服务实例，修改配置
 * @Date:2018年12月04日
 * @version:1.0
 */
public class UpdateInsAndServiceConf2315 extends TestScmBase {
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException {
        site = ScmInfo.getSite();
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test1() throws ScmException {
        try {
            ScmConfigProperties.builder().service( site.getSiteServiceName() )
                    .instance( site.getNode().getUrl() )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            Assert.fail(
                    "instance and service is not allowed to set together" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test2() throws ScmException {
        try {
            ScmConfigProperties.builder().service( site.getSiteServiceName() )
                    .allInstance()
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            Assert.fail(
                    "instance and service is not allowed to set together" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test3() throws ScmException {
        try {
            List< String > instances = new ArrayList< String >();
            instances.add( site.getNode().getUrl() );
            ScmConfigProperties.builder().service( site.getSiteServiceName() )
                    .instances( instances )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            Assert.fail(
                    "instance and service is not allowed to set together" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test4() throws ScmException {
        try {
            List< String > services = new ArrayList< String >();
            services.add( site.getSiteServiceName() );
            ScmConfigProperties.builder().services( services )
                    .instance( site.getNode().getUrl() )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            Assert.fail(
                    "instance and service is not allowed to set together" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test5() throws ScmException {
        try {
            List< String > services = new ArrayList< String >();
            services.add( site.getSiteServiceName() );
            ScmConfigProperties.builder().services( services ).allInstance()
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            Assert.fail(
                    "instance and service is not allowed to set together" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test6() throws ScmException {
        try {
            List< String > services = new ArrayList< String >();
            services.add( site.getSiteServiceName() );
            List< String > instances = new ArrayList< String >();
            instances.add( site.getNode().getUrl() );
            ScmConfigProperties confProp = ScmConfigProperties.builder()
                    .services( services ).instances( instances )
                    .updateProperty( ConfigCommonDefind.scm_audit_mask, "ALL" )
                    .updateProperty( ConfigCommonDefind.scm_audit_userMask,
                            "LOCAL" )
                    .build();
            Assert.fail(
                    "instance and service is not allowed to set together" );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT ) {
                Assert.fail( e.getMessage() );
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
    }
}
