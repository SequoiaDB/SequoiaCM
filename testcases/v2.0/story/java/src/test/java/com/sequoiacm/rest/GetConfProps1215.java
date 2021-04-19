package com.sequoiacm.rest;

import org.springframework.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @FileName SCM-1215: 获取配置属性
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class GetConfProps1215 extends TestScmBase {
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
                .setApi( "conf-properties?keys=scm.rootsite.meta.url,scm"
                        + ".rootsite.meta.user,an.inexistent.key" )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONObject conf = JSON.parseObject( response ).getJSONObject( "conf" );
        Assert.assertNotEquals( "null",
                conf.getString( "scm.rootsite.meta.url" ) );
        Assert.assertNotEquals( "null",
                conf.getString( "scm.rootsite.meta.user" ) );
        Assert.assertEquals( null, conf.getString( "an.inexistent.key" ) );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( rest != null ) {
            rest.disconnect();
        }
    }
}
