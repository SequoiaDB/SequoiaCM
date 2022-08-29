package com.sequoiacm.rest;

import com.sequoiacm.testcommon.listener.GroupTags;
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

;

/**
 * @FileName SCM-1217: 获取节点信息列表
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class ListNode1217 extends TestScmBase {
    private SiteWrapper site = null;
    private RestWrapper rest = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        int siteId = ScmInfo.getSite().getSiteId();
        String response = rest.setRequestMethod( HttpMethod.GET )
                .setApi( "nodes?filter={uri}" )
                .setUriVariables(
                        new Object[] { "{\"site_id\":" + siteId + "}" } )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONArray results = JSON.parseArray(  response );
        if ( results.size() <= 0 ) {
            Assert.fail( "no node of site_id[" + siteId + "]" );
        }
        for ( int i = 0; i < results.size(); ++i ) {
            JSONObject obj = results.getJSONObject( i );
            Assert.assertTrue( obj.containsKey( "name" ) );
            Assert.assertTrue( obj.containsKey( "id" ) );
            Assert.assertTrue( obj.containsKey( "type" ) );
            Assert.assertTrue( obj.containsKey( "site_id" ) );
        }

        response = rest.setRequestMethod( HttpMethod.GET )
                .setApi( "nodes?filter={uri}" )
                .setUriVariables( new Object[] {
                        "{\"name\":\"nobody_use_this_node_name1217\"}" } )
                .setResponseType( String.class ).exec().getBody().toString();
        Assert.assertEquals( "[]", response );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( rest != null ) {
            rest.disconnect();
        }
    }
}
