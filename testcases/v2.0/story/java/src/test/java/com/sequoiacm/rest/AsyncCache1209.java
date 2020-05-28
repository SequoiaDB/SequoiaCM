package com.sequoiacm.rest;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
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
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @FileName SCM-1209: 异步缓存文件
 * @Author linsuqiang
 * @Date 2018-04-11
 * @Version 1.00
 */

public class AsyncCache1209 extends TestScmBase {
    private final String authorName = "这是一个中文名1209";
    private RestWrapper rootRest = null;
    private RestWrapper branchRest = null;
    private WsWrapper ws = null;
    private File localPath = null;
    private String filePath = null;
    private String fileId = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile.txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, 0 );
        branchSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        ws = ScmInfo.getWs();
        branchRest = new RestWrapper();
        branchRest.connect( branchSite.getSiteServiceName(),
                TestScmBase.scmUserName, TestScmBase.scmPassword );
        rootRest = new RestWrapper();
        rootRest.connect( rootSite.getSiteServiceName(),
                TestScmBase.scmUserName, TestScmBase.scmPassword );
        fileId = createFile( rootRest, ws, filePath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        String response = branchRest.setRequestMethod( HttpMethod.POST )
                .setApi( "files/" + fileId + "/async-cache?workspace_name="
                        + ws.getName() )
                .setResponseType( String.class ).exec().getBody().toString();
        Assert.assertEquals( "\"\"", response );
        checkAsyncCache( branchRest, ws, fileId );

        try {
            String inexistentId = "ffffffffffffffff";
            response = branchRest.setRequestMethod( HttpMethod.POST )
                    .setApi( "files/" + inexistentId
                            + "/async-cache?workspace_name=" + ws.getName() )
                    .setResponseType( String.class ).exec().getBody()
                    .toString();
            Assert.fail( "async-transfer should not succeed" );
        } catch ( HttpClientErrorException e ) {
            Assert.assertEquals( e.getStatusCode().value(), 400 );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        deleteFile( rootRest, ws, fileId );
        if ( rootRest != null ) {
            rootRest.disconnect();
        }
        if ( branchRest != null ) {
            branchRest.disconnect();
        }
    }

    private String createFile( RestWrapper rest, WsWrapper ws, String filePath )
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
        String fileId = JSON.parseObject( wResponse ).getJSONObject( "file" )
                .getString( "id" );
        return fileId;
    }

    private void deleteFile( RestWrapper rest, WsWrapper ws, String fileId ) {
        rest.setRequestMethod( HttpMethod.DELETE )
                .setApi( "files/" + fileId + "?workspace_name=" + ws.getName()
                        + "&is_physical=true" )
                .setResponseType( Resource.class ).exec();
    }

    private void checkAsyncCache( RestWrapper rest, WsWrapper ws,
            String fileId ) throws Exception {
        int times = 120;
        int intervalSec = 1;
        boolean checkOk = false;
        for ( int i = 0; i < times; ++i ) {
            String response = rest.setRequestMethod( HttpMethod.HEAD )
                    .setApi( "files/id/" + fileId + "?workspace_name="
                            + ws.getName() )
                    .setResponseType( String.class ).exec().getHeaders()
                    .get( "file" ).toString();
            response = URLDecoder.decode( response, "UTF-8" );
            JSONObject fileInfo = JSON.parseArray( response )
                    .getJSONObject( 0 );
            JSONArray siteList = ( JSONArray ) fileInfo.get( "site_list" );
            if ( siteList.size() >= 2 ) {
                checkOk = true;
                break;
            } else {
                Thread.sleep( intervalSec * 1000 );
            }
        }
        if ( !checkOk ) {
            throw new Exception( "fail to async-cache" );
        }
    }
}
