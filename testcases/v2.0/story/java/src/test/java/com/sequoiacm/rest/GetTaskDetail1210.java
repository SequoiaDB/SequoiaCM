package com.sequoiacm.rest;

import org.junit.Assert;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1210: 获取任务详情
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class GetTaskDetail1210 extends TestScmBase {
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

        JSONObject options = JSON.parseObject((
                "{ 'filter': { 'author': 'inexistent_author1210' } }" ));
        String response = rest.setRequestMethod( HttpMethod.POST )
                .setApi( "tasks" ).setParameter( "task_type", "2" )
                .setParameter( "workspace_name", ws.getName() )
                .setParameter( "options", options.toString() )
                .setResponseType( String.class ).exec().getBody().toString();
        taskId = JSON.parseObject( response ).getJSONObject( "task" )
                .getString( "id" );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        String response = rest.setRequestMethod( HttpMethod.HEAD )
                .setApi( "tasks/" + taskId ).setResponseType( String.class )
                .exec().getHeaders().get( "task" ).toString();
        JSONObject obj = JSON.parseArray( response ).getJSONObject( 0 );
        Assert.assertEquals( taskId, obj.getString( "id" ) );

        try {
            String inexistentId = "ffffffffffffffff";
            response = rest.setRequestMethod( HttpMethod.HEAD )
                    .setApi( "tasks/" + inexistentId )
                    .setResponseType( String.class ).exec().getBody()
                    .toString();
            Assert.fail( "getting inexistent task detail should not succeed" );
        } catch ( HttpClientErrorException e ) {
            Assert.assertEquals( 404, e.getStatusCode().value() );
        }
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
