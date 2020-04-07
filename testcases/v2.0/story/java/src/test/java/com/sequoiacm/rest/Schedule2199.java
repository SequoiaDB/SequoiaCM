package com.sequoiacm.rest;

import org.bson.BSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.sequoiacm.client.common.ScheduleType;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-2199:异步任务调度接口测试
 * @author FanYu
 * @Date:2018年3月21日
 * @version:1.0
 */
public class Schedule2199 extends TestScmBase {
    private RestWrapper rest = null;
    private SiteWrapper site = null;
    private SiteWrapper site1 = null;
    private String name = "Schedule2199";
    private WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws JSONException {
        site = ScmInfo.getBranchSite();
        site1 = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        // create schedule
        BSONObject cond = ScmQueryBuilder.start().and( "source_site" )
                .is( site.getSiteName() ).and( "target_site" )
                .is( site1.getSiteName() ).and( "max_stay_time" ).is( "1d" )
                .get();

        JSONObject desc = new JSONObject();
        desc.put( "name", name );
        desc.put( "desc", name );
        desc.put( "type", ScheduleType.COPY_FILE.getName() );
        desc.put( "workspace", wsp.getName() );
        desc.put( "content", cond );
        desc.put( "cron", "* * * * * ? 2029" );
        String response = rest.setServerType( "schedule-server" )
                .setRequestMethod( HttpMethod.POST ).setApi( "/schedules" )
                .setParameter( "description", desc.toString() )
                .setResponseType( String.class ).exec().getBody().toString();
        System.out.println( "response = " + response );
        JSONObject schduleInfo = new JSONObject( response );
        String schduleId = schduleInfo.getString( "id" );

        // get schedule
        String response1 = rest.setServerType( "schedule-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/schedules/" + schduleId )
                .setResponseType( String.class ).exec().getBody().toString();
        System.out.println( "response = " + response1 );
        JSONObject schduleInfo1 = new JSONObject( response1 );
        Assert.assertEquals( schduleInfo1.getString( "id" ), schduleId );
        Assert.assertEquals( schduleInfo1.getString( "desc" ), name );
        Assert.assertEquals( schduleInfo1.getString( "type" ),
                ScheduleType.COPY_FILE.getName() );
        Assert.assertEquals( schduleInfo1.getString( "workspace" ),
                wsp.getName() );

        // update schedule
        JSONObject desc1 = new JSONObject();
        desc1.put( "name", name + "_update" );
        desc1.put( "desc", name + "_update" );
        String response2 = rest.setServerType( "schedule-server" )
                .setRequestMethod( HttpMethod.PUT )
                .setApi( "/schedules/" + schduleId )
                .setParameter( "description", desc1.toString() )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONObject schduleInfo2 = new JSONObject( response2 );
        Assert.assertEquals( schduleInfo2.getString( "id" ), schduleId );
        Assert.assertEquals( schduleInfo2.getString( "desc" ),
                name + "_update" );
        Assert.assertEquals( schduleInfo2.getString( "name" ),
                name + "_update" );

        // get schedule
        String response3 = rest.setServerType( "schedule-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/schedules/" + schduleId )
                .setResponseType( String.class ).exec().getBody().toString();
        System.out.println( "response = " + response3 );
        JSONObject schduleInfo3 = new JSONObject( response3 );
        Assert.assertEquals( schduleInfo3.getString( "id" ), schduleId );
        Assert.assertEquals( schduleInfo3.getString( "desc" ),
                name + "_update" );
        Assert.assertEquals( schduleInfo3.getString( "name" ),
                name + "_update" );
        Assert.assertEquals( schduleInfo3.getString( "type" ),
                ScheduleType.COPY_FILE.getName() );
        Assert.assertEquals( schduleInfo3.getString( "workspace" ),
                wsp.getName() );

        // list schedules
        String response4 = rest.setServerType( "schedule-server" )
                .setRequestMethod( HttpMethod.GET ).setApi( "/schedules" )
                .setParameter( "filter", desc1.toString() )
                .setResponseType( String.class ).exec().getBody().toString();
        System.out.println( "response = " + response4 );
        JSONArray schedules = new JSONArray( response4 );
        Assert.assertEquals( schedules.length() >= 1, true,
                schedules.toString() );

        // delete schedule
        rest.setServerType( "schedule-server" )
                .setRequestMethod( HttpMethod.DELETE )
                .setApi( "/schedules/" + schduleId )
                .setResponseType( String.class ).exec();

        // check delete
        try {
            rest.setServerType( "schedule-server" )
                    .setRequestMethod( HttpMethod.GET )
                    .setApi( "/schedules/" + schduleId )
                    .setResponseType( String.class ).exec();
            Assert.fail( "exp fail but act success" );
        } catch ( HttpClientErrorException | HttpServerErrorException e ) {
            Assert.assertEquals( e.getStatusCode().value(),
                    ScmError.HTTP_NOT_FOUND.getErrorCode(), e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( rest != null ) {
            rest.setServerType( "content-server" ).disconnect();
        }
    }
}
