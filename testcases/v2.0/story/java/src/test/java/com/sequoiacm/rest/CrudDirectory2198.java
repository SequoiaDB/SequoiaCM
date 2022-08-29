package com.sequoiacm.rest;

import java.net.URLDecoder;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-2198:文件夹接口测试
 * @Author fanyu
 * @Date 2018-04-11
 * @Version 1.00
 */

public class CrudDirectory2198 extends TestScmBase {
    private RestWrapper rest = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private String rootId = "000000000000000000000000";
    private String fullPath1 = "/CrudDirectory2198_A";
    private String fullPath2 = "/CrudDirectory2198_B";
    private String id1 = null;
    private String id2 = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {
        // create directory by fullPath
        String response = rest.setRequestMethod( HttpMethod.POST )
                .setApi( "/directories" )
                .setParameter( "workspace_name", wsp.getName() )
                .setParameter( "name", "CrudDirectory2198_A" )
                .setParameter( "path", fullPath1 )
                .setResponseType( String.class ).exec().getBody().toString();
        id1 = JSON.parseObject( response ).getString( "id" );

        // create directory by parentId + name
        String response1 = rest.setRequestMethod( HttpMethod.POST )
                .setApi( "/directories" )
                .setParameter( "workspace_name", wsp.getName() )
                .setParameter( "name", "CrudDirectory2198_B" )
                .setParameter( "parent_directory_id", rootId )
                .setResponseType( String.class ).exec().getBody().toString();
        id2 = JSON.parseObject( response1 ).getString( "id" );

        // get directory by Id
        String response2 = rest.setRequestMethod( HttpMethod.HEAD )
                .setApi( "/directories/id/" + id1 + "?workspace_name="
                        + wsp.getName() )
                .setResponseType( String.class ).exec().getHeaders()
                .getFirst( "directory" );
        response2 = URLDecoder.decode( response2, "UTF-8" );
        JSONObject dirInfo1 = JSON.parseObject( response2 );
        Assert.assertEquals( dirInfo1.getString( "name" ),
                "CrudDirectory2198_A" );
        Assert.assertEquals( dirInfo1.getString( "id" ), id1 );
        Assert.assertEquals( dirInfo1.getString( "parent_directory_id" ),
                rootId );

        // get directory by path
        String response3 = rest.setRequestMethod( HttpMethod.HEAD )
                .setApi( "/directories/path/" + fullPath2 + "?workspace_name="
                        + wsp.getName() )
                .setResponseType( String.class ).exec().getHeaders()
                .getFirst( "directory" );
        response3 = URLDecoder.decode( response3, "UTF-8" );
        JSONObject dirInfo2 = JSON.parseObject( response3 );
        Assert.assertEquals( dirInfo2.getString( "name" ),
                "CrudDirectory2198_B" );
        Assert.assertEquals( dirInfo2.getString( "id" ), id2 );
        Assert.assertEquals( dirInfo2.getString( "parent_directory_id" ),
                rootId );

        // get path by directory id
        String response4 = rest.setRequestMethod( HttpMethod.GET )
                .setApi( "/directories/id/" + id1 + "/path?workspace_name="
                        + wsp.getName() )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONObject dirInfo3 = JSON.parseObject( response4 );
        Assert.assertEquals( dirInfo3.getString( "path" ), fullPath1 + "/" );

        // list directory
        JSONObject desc1 = new JSONObject();
        desc1.put( "id", id1 );
        String response5 = rest.setRequestMethod( HttpMethod.GET )
                .setApi( "/metadatas/classes?workspace_name=" + wsp.getName()
                        + "&filter={uri}" )
                .setUriVariables( new Object[] { desc1.toString() } ).exec()
                .getBody().toString();
        JSONArray dirInfo4 = JSON.parseArray( response5 );
        System.out.println( "dirInfo3 = " + dirInfo4 );

        // rename directory by id
        String newName = "CrudDirectory2198_A_New";
        String response6 = rest.setRequestMethod( HttpMethod.PUT )
                .setApi( "/directories/id/" + id1 + "/rename" )
                .setParameter( "workspace_name", wsp.getName() )
                .setParameter( "name", newName ).exec().getBody().toString();
        JSONObject dirInfo5 = JSON.parseObject( response6 );
        Assert.assertNotNull( dirInfo5 );

        // rename directory by path
        String newName1 = "CrudDirectory2198_B_New";
        String response7 = rest.setRequestMethod( HttpMethod.PUT )
                .setApi( "/directories/path/" + "/CrudDirectory2198_B"
                        + "/rename" )
                .setParameter( "workspace_name", wsp.getName() )
                .setParameter( "name", newName1 ).exec().getBody().toString();
        JSONObject dirInfo6 = JSON.parseObject( response7 );
        Assert.assertNotNull( dirInfo6 );

        // move directory by id
        String response8 = rest.setRequestMethod( HttpMethod.PUT )
                .setApi( "/directories/id/" + id1 + "/move" )
                .setParameter( "parent_directory_path",
                        "/CrudDirectory2198_B_New" )
                .setParameter( "workspace_name", wsp.getName() ).exec()
                .getBody().toString();
        JSONObject dirInfo7 = JSON.parseObject( response8 );
        Assert.assertNotNull( dirInfo7 );

        // move directory by path
        String response9 = rest.setRequestMethod( HttpMethod.PUT )
                .setApi( "/directories/path/"
                        + "/CrudDirectory2198_B_New/CrudDirectory2198_A_New"
                        + "/move" )
                .setParameter( "parent_directory_path", "/" )
                .setParameter( "workspace_name", wsp.getName() ).exec()
                .getBody().toString();
        JSONObject dirInfo8 = JSON.parseObject( response9 );
        Assert.assertNotNull( dirInfo8 );

        // delete directory by path
        rest.setRequestMethod( HttpMethod.DELETE ).setApi( "/directories/path/"
                + "/CrudDirectory2198_B_New?workspace_name=" + wsp.getName() )
                .exec();

        rest.setRequestMethod( HttpMethod.DELETE ).setApi(
                "/directories/id/" + id1 + "?workspace_name=" + wsp.getName() )
                .exec();

        // check delete
        try {
            rest.setRequestMethod( HttpMethod.GET )
                    .setApi( "/directories/id/" + id1 + "/path?workspace_name="
                            + wsp.getName() )
                    .setResponseType( String.class ).exec()
                    .getStatusCodeValue();
        } catch ( HttpClientErrorException | HttpServerErrorException e ) {
            Assert.assertEquals( e.getStatusCode().value(),
                    ScmError.HTTP_NOT_FOUND.getErrorCode(), e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        if ( rest != null ) {
            rest.disconnect();
        }
    }
}
