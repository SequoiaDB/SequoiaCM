package com.sequoiacm.rest;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.util.json.JSONObject;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1206: 获取文件总数
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class CountFiles1206 extends TestScmBase {
    private final int fileNum = 5;
    private final int fileSize = 0;
    // private final String authorName = "这是一个中文名1206";
    private final String authorName = "1206";
    private final List< String > fileIdList = new ArrayList<>( fileSize );
    private RestWrapper rest = null;
    private WsWrapper ws = null;
    private File localPath = null;
    private String filePath = null;
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        ws = ScmInfo.getWs();
        site = ScmInfo.getRootSite();
        rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        for ( int i = 0; i < fileNum; i++ ) {
            String fileId = createFile( ws, filePath );
            fileIdList.add( fileId );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        String response = rest.setRequestMethod( HttpMethod.HEAD )
                .setApi( "files?workspace_name=" + ws.getName()
                        + "&filter={uri}" )
                .setUriVariables(
                        new Object[] { "{\"author\":\"" + authorName + "\"}" } )
                .setResponseType( String.class ).exec().getHeaders()
                .get( "X-SCM-Count" ).toString();
        System.out.println( response.toString() );
        Assert.assertEquals( response, "[5]" );

        try {
            response = rest.setRequestMethod( HttpMethod.HEAD )
                    .setApi( "files?workspace_name=inexistent_ws_name1206"
                            + "&filter={uri}" )
                    .setUriVariables( new Object[] {
                            "{\"author\":\"" + authorName + "\"}" } )
                    .setResponseType( String.class ).exec().getHeaders()
                    .get( "X-SCM-Count" ).toString();
            Assert.fail( "count files of inexistent ws should not succeed" );
        } catch ( HttpClientErrorException | HttpServerErrorException e ) {
            Assert.assertEquals( e.getStatusCode().value(),
                    ScmError.HTTP_UNAUTHORIZED.getErrorCode() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        for ( int i = 0; i < fileNum; i++ ) {
            deleteFile( ws, fileIdList.get( i ) );
        }
        if ( rest != null ) {
            rest.disconnect();
        }
    }

    private String createFile( WsWrapper ws, String filePath )
            throws Exception {
        JSONObject desc = new JSONObject();
        desc.put( "name", this.getClass().getSimpleName() + UUID.randomUUID() );
        desc.put( "author", authorName );
        desc.put( "title", authorName );
        desc.put( "mime_type", "text/plain" );
        File file = new File( filePath );
        // FileSystemResource resource = new FileSystemResource(file);
        String wResponse = rest.setRequestMethod( HttpMethod.POST )
                .setApi( "files?workspace_name=" + ws.getName() )
                // .setParameter("file", resource)
                // .setParameter("description", desc.toString())
                .setRequestHeaders( "description", desc.toString() )
                .setInputStream( new FileInputStream( file ) )
                .setResponseType( String.class ).exec().getBody().toString();
        String fileId = new JSONObject( wResponse ).getJSONObject( "file" )
                .getString( "id" );
        return fileId;
    }

    private void deleteFile( WsWrapper ws, String fileId ) {
        rest.setRequestMethod( HttpMethod.DELETE )
                .setApi( "files/" + fileId + "?workspace_name=" + ws.getName()
                        + "&is_physical=true" )
                .setResponseType( Resource.class ).exec();
    }
}
