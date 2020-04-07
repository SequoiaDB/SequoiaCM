package com.sequoiacm.rest;

import org.springframework.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONObject;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @FileName SCM-1216: 获取单个配置属性
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class GetOneConfProps1216 extends TestScmBase {
    private RestWrapper rest = null;
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getRootSite();
        rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        String response = rest.setRequestMethod( HttpMethod.GET )
                .setApi( "conf-properties/scm.rootsite.meta.url" )
                .setResponseType( String.class ).exec().getBody().toString();
        System.out.println( "---" + response );
        JSONObject conf = new JSONObject( response );
        Assert.assertEquals( 1, conf.length() );
        Assert.assertNotEquals( "null",
                conf.getString( "scm.rootsite.meta.url" ) );

        response = rest.setRequestMethod( HttpMethod.GET )
                .setApi( "conf-properties/ke" )
                .setResponseType( String.class ).exec().getBody().toString();
        conf = new JSONObject( response );
        Assert.assertEquals( 1, conf.length() );
        Assert.assertEquals( "null", conf.getString( "ke" ) );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( rest != null ) {
            rest.disconnect();
        }
    }
}
