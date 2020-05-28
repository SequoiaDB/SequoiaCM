package com.sequoiacm.rest;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
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
 * @FileName SCM-1207: 更新文件信息
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class UpdateFileInfo1207 extends TestScmBase {
    private RestWrapper rest = null;
    private WsWrapper ws = null;
    private File localPath = null;
    private String filePath = null;
    private String fileId = null;
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile.txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, 0 );
        site = ScmInfo.getRootSite();
        ws = ScmInfo.getWs();
        rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        fileId = createFile( ws, filePath );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testChinese() throws Exception {
        String response = rest.setRequestMethod( HttpMethod.PUT )
                .setApi( "files/" + fileId + "?workspace_name=" + ws.getName()
                        + "&major_version=1&minor_version=0" )
                .setParameter( "file_info", "{ author: '新的中文名1207' }" )
                .setResponseType( String.class ).exec().getHeaders()
                .get( "file_info" ).toString();
        String fileInfo = URLDecoder.decode( response, "UTF-8" );
        JSONObject obj = new JSONObject(
                fileInfo.substring( 1, fileInfo.length() - 1 ) );
        Assert.assertEquals( obj.getString( "author" ), "新的中文名1207" );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void testInvaild() throws Exception {
        try {
            rest.setRequestMethod( HttpMethod.PUT )
                    .setApi( "files/" + fileId + "?ws_name=" + ws.getName()
                            + "&major_version=1&minor_version=0" )
                    .setParameter( "file_info", "{ author: '' }" )
                    .setResponseType( String.class ).exec().getHeaders()
                    .toString();
            Assert.fail( "updating file without necessary argument should not "
                    + "succeed" );
        } catch ( HttpServerErrorException | HttpClientErrorException e ) {
            Assert.assertEquals( e.getStatusCode().value(),
                    ScmError.HTTP_BAD_REQUEST.getErrorCode(), e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        deleteFile( ws, fileId );
        if ( rest != null ) {
            rest.disconnect();
        }
    }

    private String createFile( WsWrapper ws, String filePath )
            throws Exception {
        JSONObject desc = new JSONObject();
        desc.put( "name", this.getClass().getSimpleName() + UUID.randomUUID() );
        desc.put( "author", this.getClass().getSimpleName() );
        desc.put( "title", this.getClass().getSimpleName() );
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
