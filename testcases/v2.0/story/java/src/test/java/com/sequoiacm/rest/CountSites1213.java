package com.sequoiacm.rest;

import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @FileName SCM-1213: 获取站点总数
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class CountSites1213 extends TestScmBase {
    private RestWrapper rest = null;
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();

        rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
    }

    //TODO: rest interface not implemented
    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void test() throws Exception {
        String response = rest.setRequestMethod( HttpMethod.HEAD )
                .setApi( "sites" )
                .setResponseType( String.class ).exec().getBody().toString();
        System.out.println( response );

        response = rest.setRequestMethod( HttpMethod.HEAD )
                .setApi( "site" )
                .setResponseType( String.class ).exec().getBody().toString();
        System.out.println( response );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( rest != null ) {
            rest.disconnect();
        }
    }
}
