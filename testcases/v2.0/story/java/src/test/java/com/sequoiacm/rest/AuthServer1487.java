package com.sequoiacm.rest;

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
import com.sequoiacm.client.core.ScmUserPasswordType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;

/**
 * @Description:SCM-1487:创建用户，认证方式默认
 *              SCM-1515:查询指定用户
 *              SCM-1811:增删改查用户/角色/登录登出/会话
 * @author fanyu
 * @Date:2018年3月21日
 * @version:1.0
 */
public class AuthServer1487 extends TestScmBase {
    private RestWrapper rest = null;
    private RestWrapper rest1 = null;
    private SiteWrapper site = null;
    private String username = "AuthServer1487";
    private String password = "1487";

    @BeforeClass(alwaysRun = true)
    private void setUp() throws JSONException {
        site = ScmInfo.getSite();
        rest = new RestWrapper();
        rest1 = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
    }

    @Test
    private void test() throws Exception {
        //1487 create user
        String response = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.POST )
                .setApi( "/users/" + username )
                .setParameter( "password", password )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONObject userInfo = new JSONObject( response );
        Assert.assertEquals( userInfo.getString( "username" ), username );
        Assert.assertEquals( userInfo.getString( "password_type" ),
                ScmUserPasswordType.LOCAL.name() );

        //1515 get User
        String response1 = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/users/" + username )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONObject userInfo1 = new JSONObject( response1 );
        Assert.assertEquals( userInfo1.getString( "username" ), username );
        Assert.assertEquals( userInfo1.getString( "password_type" ),
                ScmUserPasswordType.LOCAL.name() );

        //list users
        String response2 = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/users?password_type=" + ScmUserPasswordType.LOCAL )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONArray userInfo2 = new JSONArray( response2 );
        Assert.assertEquals( userInfo2.length() >= 1, true,
                userInfo2.toString() );

        //create role
        String response3 = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.POST )
                .setApi( "/roles/" + username )
                .setParameter( "description", username )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONObject roleInfo1 = new JSONObject( response3 );
        Assert.assertEquals( roleInfo1.getString( "role_name" ),
                "ROLE_" + username );

        //get role
        String response4 = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/roles/" + username )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONObject roleInfo2 = new JSONObject( response4 );
        Assert.assertEquals( roleInfo2.getString( "role_name" ),
                "ROLE_" + username );
        Assert.assertEquals( roleInfo2.getString( "description" ), username );

        //list role
        String response5 = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/roles" )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONArray roles = new JSONArray( response5 );
        Assert.assertEquals( roles.length() >= 1, true, roles.toString() );

        //user attach role
        String response6 = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.PUT )
                .setApi( "/users/" + username )
                .setParameter( "add_roles", username )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONObject userInfo3 = new JSONObject( response6 );
        Assert.assertEquals( userInfo3.getJSONArray( "roles" ).length(), 1 );

        //login
        rest1.connect( site.getSiteServiceName(), username, password );

        //list sessions
        String response7 = rest1.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/sessions?username=" + username )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONArray sessions = new JSONArray( response7 );
        Assert.assertEquals( sessions.length(), 1, sessions.toString() );
        String sessionId = sessions.getJSONObject( 0 )
                .getString( "session_id" );

        //get session
        String response8 = rest1.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/sessions/" + sessionId + "?user_details=" + true )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONObject sessionInfo = new JSONObject( response8 );
        Assert.assertEquals( sessionInfo.getString( "username" ), username );

        // delete session
        rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.DELETE )
                .setApi( "/sessions/" + sessionId )
                .setResponseType( String.class ).exec();
        try {
            rest.setServerType( "auth-server" )
                    .setRequestMethod( HttpMethod.GET )
                    .setApi(
                            "/sessions/" + sessionId + "?user_details=" + true )
                    .setResponseType( String.class ).exec();
            Assert.fail( "exp fail but act success,sessionId = " + sessionId );
        } catch ( HttpClientErrorException | HttpServerErrorException e ) {
            if ( e.getStatusCode().value() !=
                    ScmError.HTTP_NOT_FOUND.getErrorCode() ) {
                throw e;
            }
        }

        // delete role
        rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.DELETE )
                .setApi( "/roles/" + username )
                .setResponseType( String.class ).exec();
        try {
            rest.setServerType( "auth-server" )
                    .setRequestMethod( HttpMethod.GET )
                    .setApi( "/roles/" + username )
                    .setResponseType( String.class ).exec();
            Assert.fail( "exp fail but act success,roleName = " + username );
        } catch ( HttpClientErrorException | HttpServerErrorException e ) {
            if ( e.getStatusCode().value() !=
                    ScmError.HTTP_NOT_FOUND.getErrorCode() ) {
                throw e;
            }
        }

        // delete users
        rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.DELETE )
                .setApi( "/users/" + username )
                .setResponseType( String.class ).exec();
        try {
            rest.setServerType( "auth-server" )
                    .setRequestMethod( HttpMethod.GET )
                    .setApi( "/users/" + username )
                    .setResponseType( String.class ).exec();
            Assert.fail( "exp fail but act Success,username = " + username );
        } catch ( HttpClientErrorException | HttpServerErrorException e ) {
            if ( e.getStatusCode().value() !=
                    ScmError.HTTP_NOT_FOUND.getErrorCode() ) {
                throw e;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( rest != null ) {
            rest.setServerType( "content-server" ).disconnect();
        }
    }
}
