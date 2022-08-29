/**
 *
 */
package com.sequoiacm.rest;

import java.util.List;

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
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-1099 :: 获取工作区列表/获取工作区信息
 * @author fanyu
 * @Date:2018年3月21日
 * @version:1.0
 */
public class GetWorkSpace1099 extends TestScmBase {
    private WsWrapper ws = null;
    private RestWrapper rest = null;
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        ws = ScmInfo.getWs();
        site = ScmInfo.getRootSite();
        rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        // getwsList(match all)
        String response1 = rest.setApi( "workspaces" )
                .setRequestMethod( HttpMethod.GET )
                .setParameter( "filter", "{ name: { $exist: 1 } }" )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONArray wsListInfo = JSON.parseArray( response1 );
        List< WsWrapper > wsList = ScmInfo.getAllWorkspaces();
        // just check num
        Assert.assertTrue( wsList.size() <= wsListInfo.size(),
                "wsListByRest = " + wsListInfo.toString() + ",wsListByDb = "
                        + wsList.toString() );

        // getwsList(match none)
        response1 = rest.setRequestMethod( HttpMethod.GET )
                .setApi( "workspaces?filter={uri}" )
                .setUriVariables( new Object[] {
                        "{\"name\":\"inexistent_ws_name1099\"}" } )
                .setResponseType( String.class ).exec().getBody().toString();
        wsListInfo = JSON.parseArray( response1 );
        // just check num
        Assert.assertEquals( 0, wsListInfo.size(),
                "no ws should be returned" );

        // check getws
        String response2 = rest.reset().setApi( "workspaces/" + ws.getName() )
                .setRequestMethod( HttpMethod.GET )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONObject wsInfo = JSON.parseObject( response2 )
                .getJSONObject( "workspace" );
        // check
        Assert.assertEquals( wsInfo.get( "name" ), ws.getName(),
                "wsByRest = " + wsInfo + ",wsByDb = " + ws.toString() );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( rest != null ) {
            rest.disconnect();
        }
    }
}
