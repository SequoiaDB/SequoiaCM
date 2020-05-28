package com.sequoiacm.rest;

import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONObject;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1211: 获取任务总数
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class CountTasks1211 extends TestScmBase {
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
                new JSONObject().put( "author", "inexistent_author1211" ) );
        String response = rest.setRequestMethod( HttpMethod.POST )
                .setApi( "tasks" ).setParameter( "task_type", "2" )
                .setParameter( "workspace_name", ws.getName() )
                .setParameter( "options", options.toString() )
                .setResponseType( String.class ).exec().getBody().toString();
        taskId = new JSONObject( response ).getJSONObject( "task" )
                .getString( "id" );
    }

    // TODO: rest interface not implemented
    @Test(groups = { "oneSite", "twoSite", "fourSite" }, enabled = false)
    private void test() throws Exception {
        String response = rest.setRequestMethod( HttpMethod.HEAD )
                .setApi( "tasks" ).setResponseType( String.class ).exec()
                .getBody().toString();
        System.out.println( response );

        response = rest.setRequestMethod( HttpMethod.HEAD ).setApi( "tasks" )
                .setResponseType( String.class ).exec().getBody().toString();
        System.out.println( response );
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
