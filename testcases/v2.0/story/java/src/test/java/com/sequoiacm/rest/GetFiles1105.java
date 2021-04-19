package com.sequoiacm.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description:SCM-1105 :: 获取文件列表/获取文件信息
 * @author fanyu
 * @Date:2018年3月22日
 * @version:1.0
 */
public class GetFiles1105 extends TestScmBase {
    private boolean runSuccess = false;
    private WsWrapper ws = null;
    private RestWrapper rest = null;
    private File localPath = null;
    private int fileNum = 5;
    private String filePath = null;
    private int fileSize = 0;
    private String author = "GetFiles1105";
    private List< String > fileIdList = new ArrayList< String >();
    private JSONArray descs = new JSONArray();
    private SiteWrapper site = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            localPath = new File( TestScmBase.dataDirectory + File.separator
                    + TestTools.getClassName() );
            filePath = localPath + File.separator + "localFile_" + fileSize
                    + ".txt";
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );
            ws = ScmInfo.getWs();
            site = ScmInfo.getRootSite();
            rest = new RestWrapper();
            rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
            for ( int i = 0; i < fileNum; i++ ) {
                JSONObject desc = new JSONObject();
                String name = author + "_" + i + "_" + UUID.randomUUID();
                desc.put( "name", name );
                System.out.println( "name = " + name );
                desc.put( "author", author );
                desc.put( "title", author );
                desc.put( "mime_type", "text/plain" );
                String fileId = upload( filePath, ws, desc.toString() );
                fileIdList.add( fileId );
                descs.add( desc );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // just check num
        String fileMetaList = rest.setRequestMethod( HttpMethod.GET )
                .setApi( "files?workspace_name=" + ws.getName() + "&scope="
                        + ScopeType.SCOPE_CURRENT.getScope() + "&filter={uri}" )
                .setUriVariables(
                        new Object[] { "{\"author\":\"" + author + "\"}" } )
                .setResponseType( String.class ).exec().getBody().toString();
        JSONArray fileArr = JSON.parseArray( fileMetaList );
        Assert.assertEquals(
                fileArr.size() > fileNum || fileArr.size() == fileNum, true,
                "fileListInfo.getJSONArray('files').length = "
                        + fileArr.size() + ",fileNum = " + fileNum );

        int index = fileNum - 1;
        String fileMeta = rest
                .setApi( "files/id/" + fileIdList.get( index )
                        + "?workspace_name=" + ws.getName() + "&scope="
                        + ScopeType.SCOPE_CURRENT.getScope() )
                .setRequestMethod( HttpMethod.HEAD )
                .setResponseType( String.class ).exec().getHeaders()
                .get( "file" ).toString();
        fileMeta = URLDecoder.decode( fileMeta, "UTF-8" );
        JSONObject fileInfo = JSON.parseObject(
                fileMeta.substring( 1, fileMeta.length() - 1 ) );
        JSONObject desc1 = ( JSONObject ) descs.get( index );
        Assert.assertEquals( fileInfo.getString( "name" ),
                desc1.getString( "name" ) );
        Assert.assertEquals( fileInfo.getString( "author" ),
                desc1.getString( "author" ) );
        Assert.assertEquals( fileInfo.getString( "title" ),
                desc1.getString( "title" ) );
        Assert.assertEquals( fileInfo.getIntValue( "size" ), fileSize );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( String fileId : fileIdList ) {
                    rest.reset()
                            .setApi( "files/" + fileId + "?workspace_name="
                                    + ws.getName() + "&is_physical=true" )
                            .setRequestMethod( HttpMethod.DELETE )
                            .setResponseType( String.class ).exec();
                }
            }
            TestTools.LocalFile.removeFile( localPath );
        } finally {
            if ( rest != null ) {
                rest.disconnect();
            }
        }
    }

    public String upload( String filePath, WsWrapper ws, String desc )
            throws HttpClientErrorException,FileNotFoundException {
        File file = new File( filePath );
        // FileSystemResource resource = new FileSystemResource(file);
        String wResponse = rest.setApi( "files?workspace_name=" + ws.getName() )
                .setRequestMethod( HttpMethod.POST )
                // .setParameter("file", resource)
                // .setParameter("description", desc)
                .setRequestHeaders( "description", desc.toString() )
                .setInputStream( new FileInputStream( file ) )
                .setResponseType( String.class ).exec().getBody().toString();
        String fileId = JSON.parseObject( wResponse ).getJSONObject( "file" )
                .getString( "id" );
        return fileId;
    }
}
