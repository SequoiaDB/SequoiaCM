package com.sequoiacm.rest;

import org.springframework.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-2178:权限管理接口测试
 * @author fanyu
 * @Date:2018年3月21日
 * @version:1.0
 */
public class AuthServer2178 extends TestScmBase {
    private RestWrapper rest = null;
    private SiteWrapper site = null;
    private String username = "AuthServer2178";
    private String password = "2178";
    private WsWrapper wsp = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws JSONException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
    }

    @Test
    private void test() throws Exception {
        //create user
        rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.POST )
                .setApi( "/users/" + username )
                .setParameter( "password", password )
                .setResponseType( String.class ).exec();

        //create role
        String response3 = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.POST )
                .setApi( "/roles/" + username )
                .setParameter( "description", username )
                .setResponseType( String.class ).exec().getBody().toString();
        String roleId = new JSONObject( response3 ).getString( "role_id" );

        //grant role
        rest.setServerType( "content-server" )
                .setRequestMethod( HttpMethod.PUT )
                .setApi( "/roles/" + "ROLE_" + username + "/grant" )
                .setParameter( "resource_type", "workspace" )
                .setParameter( "resource", wsp.getName() )
                .setParameter( "privilege", "ALL" )
                .setResponseType( String.class ).exec();

        //user attach role
        rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.PUT )
                .setApi( "/users/" + username )
                .setParameter( "add_roles", username )
                .setResponseType( String.class ).exec();

        //get privilege version
        String response = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/privileges" )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONObject privileges = new JSONObject( response );
        Assert.assertEquals( privileges.getInt( "version" ) > 1, true,
                privileges.getInt( "version" ) + "" );

        //list relations
        String response1 = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/relations" )
                .setParameter( "role_id", roleId )
                .setParameter( "role_name", username )
                .setParameter( "resource_type", "workspace" )
                .setParameter( "resource", wsp.getName() )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONArray relations = new JSONArray( response1 );
        Assert.assertEquals( relations.length() > 1, true,
                relations.toString() );
        String privilegeId = null;
        for ( int i = 0; i < relations.length(); i++ ) {
            String roleId1 = relations.getJSONObject( i )
                    .getString( "role_id" );
            if ( roleId1.equals( roleId ) ) {
                privilegeId = relations.getJSONObject( i ).getString( "id" );
                break;
            }
        }

        //get relation
        String response2 = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/relations/" + privilegeId )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONObject privilegeInfo = new JSONObject( response2 );
        Assert.assertEquals( privilegeInfo.getString( "role_id" ), roleId,
                privilegeInfo.toString() );
        Assert.assertEquals( privilegeInfo.getString( "privilege" ), "ALL",
                privilegeInfo.toString() );

        //list resources
        String response4 = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/resources" )
                .setResponseType( String.class ).exec().getBody().toString();
        System.out.println( "response4 = " + response4 );
        JSONArray resources = new JSONArray( response4 );
        Assert.assertEquals( resources.length() >= 1, true,
                resources.toString() );
        String resourcesId = null;
        for ( int i = 0; i < resources.length(); i++ ) {
            String type = resources.getJSONObject( i ).getString( "type" );
            if ( type.equals( "workspace" ) ) {
                resourcesId = resources.getJSONObject( i ).getString( "id" );
                break;
            }
        }

        //get resources by id
        String response5 = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/resources/" + resourcesId )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONObject resourcesInfo = new JSONObject( response5 );
        Assert.assertEquals( resourcesInfo.getString( "type" ), "workspace",
                resourcesInfo.toString() );
        Assert.assertEquals( resourcesInfo.getString( "id" ), resourcesId,
                resourcesInfo.toString() );

        // get resources by ws
        String response6 = rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.GET )
                .setApi( "/resources/" + resourcesId )
                .setParameter( "workspace_name", wsp.getName() )
                .setResponseType( String.class ).exec().getBody().toString();
        System.out.println( "response6 = " + response6 );

        //revoke
        rest.setServerType( "content-server" )
                .setRequestMethod( HttpMethod.PUT )
                .setApi( "/roles/" + "ROLE_" + username + "/revoke" )
                .setParameter( "resource_type", "workspace" )
                .setParameter( "resource", wsp.getName() )
                .setParameter( "privilege", "ALL" )
                .setResponseType( String.class ).exec();

        // delete role
        rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.DELETE )
                .setApi( "/roles/" + username )
                .setResponseType( String.class ).exec();

        // delete users
        rest.setServerType( "auth-server" )
                .setRequestMethod( HttpMethod.DELETE )
                .setApi( "/users/" + username )
                .setResponseType( String.class ).exec();
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( rest != null ) {
            rest.setServerType( "content-server" ).disconnect();
        }
    }
}
