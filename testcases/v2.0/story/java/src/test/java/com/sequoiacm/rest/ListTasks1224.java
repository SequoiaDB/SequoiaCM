package com.sequoiacm.rest;

import org.junit.Assert;
import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONObject;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1224: 获取任务列表
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class ListTasks1224 extends TestScmBase {
    private WsWrapper ws = null;
    private RestWrapper rest = null;
    private String taskId = null;
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        ws = ScmInfo.getWs();
        site = ScmInfo.getBranchSite();
        rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );

        JSONObject options = new JSONObject().put( "filter",
                new JSONObject().put( "author", "inexistent_author1224" ) );
        String response = rest.setRequestMethod( HttpMethod.POST )
                .setApi( "tasks" )
                .setParameter( "task_type", "2" )
                .setParameter( "workspace_name", ws.getName() )
                .setParameter( "options", options.toString() )
                .setResponseType( String.class ).exec().getBody().toString();
        taskId = new JSONObject( response ).getJSONObject( "task" )
                .getString( "id" );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        String response = rest.setRequestMethod( HttpMethod.GET )
                .setApi( "tasks?filter={uri}" )
                .setUriVariables(
                        new Object[] { "{\"id\":\"" + taskId + "\"}" } )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONArray responseJSONArr = new JSONArray( response );
        int respSize = responseJSONArr.length();
        Assert.assertEquals( 1, respSize );
        String respWsName = ( ( JSONObject ) responseJSONArr.get( 0 ) )
                .getString( "workspace_name" );
        Assert.assertEquals( ws.getName(), respWsName );

        String inexistentId = "fffffffffffffff";
        response = rest.setRequestMethod( HttpMethod.GET )
                .setApi( "tasks?filter={uri}" )
                .setUriVariables(
                        new Object[] { "{\"id\":\"" + inexistentId + "\"}" } )
                .setResponseType( String.class ).exec().getBody().toString();
        respSize = new JSONArray( response ).length();
        Assert.assertEquals( 0, respSize );
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( taskId != null ) {
            TestSdbTools.Task.deleteMeta( new ScmId( taskId ) );
        }
        if ( rest != null ) {
            rest.disconnect();
        }
    }
}
