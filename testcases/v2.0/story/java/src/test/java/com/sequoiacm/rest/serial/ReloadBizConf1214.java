package com.sequoiacm.rest.serial;

import org.springframework.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @FileName SCM-1214: 重新载入业务配置
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class ReloadBizConf1214 extends TestScmBase {
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
    // create and delete workspace is very hard. as scm driver testcases have
    // tested it in detail,
    // this test just check the return value.
    private void test() throws Exception {
        String response = rest.setRequestMethod( HttpMethod.POST )
                .setApi( "/reload-bizconf" )
                .setParameter( "scope", 1 )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONArray result = JSON.parseArray( response );
        Assert.assertTrue( result.size() > 0 );
        for ( int i = 0; i < result.size(); ++i ) {
            JSONObject obj = ( JSONObject ) result.get( i );
            Assert.assertTrue( obj.containsKey( "server_id" ) );
            Assert.assertTrue( obj.containsKey( "site_id" ) );
            Assert.assertTrue( obj.containsKey( "hostname" ) );
            Assert.assertTrue( obj.containsKey( "port" ) );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( rest != null ) {
            rest.disconnect();
        }
    }
}
