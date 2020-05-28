package com.sequoiacm.rest;

import org.junit.Assert;
import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @FileName SCM-1205: 获取工作区数量
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class CountWorkspaces1205 extends TestScmBase {
    private RestWrapper rest = null;
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
    }

    // TODO: function is not implemented yet.
    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void test() throws Exception {
        Object response = rest.setRequestMethod( HttpMethod.HEAD )
                .setApi( "workspaces" ).setResponseType( String.class ).exec()
                .getHeaders().get( "X-SCM-Count" );
        // TODO: check result
        System.out.println( response );
        // JSONObject result = JSON.parseObject(response);
        int actWsNum = ( int ) response;
        Assert.assertEquals( ScmInfo.getWsNum(), actWsNum );

        response = rest.setRequestMethod( HttpMethod.GET )
                .setApi( "workspaces" ).setResponseType( String.class ).exec()
                .getHeaders().toString();
        // TODO: check result
        System.out.println( response );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( rest != null ) {
            rest.disconnect();
        }
    }
}
