package com.sequoiacm.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description: SCM-1106 :: 上传文件/下载文件/删除文件
 * @author fanyu
 * @Date:2018年3月22日
 * @version:1.0
 */
public class WRDFile1107 extends TestScmBase {
    private boolean runSuccess = false;
    private WsWrapper ws = null;
    private File localPath = null;
    private String fileId = null;
    private String filePath = null;
    private int fileSize = 1024 * 1;
    private String author = "WRDFile1107";
    private RestWrapper rest = null;
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
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "twoSite" })
    private void test() throws Exception {
        writeAndCheck();
        readAndCheck();
        deleteAndCheck();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( rest != null ) {
                rest.disconnect();
            }
        }
    }

    private void writeAndCheck() throws UnsupportedEncodingException, FileNotFoundException {
        JSONObject desc = null;
        // write
        try {
            desc = new JSONObject();
            desc.put( "name", author + UUID.randomUUID() );
            desc.put( "author", author );
            desc.put( "mime_type", "text/plain" );
            desc.put( "title", author );
            File file = new File( filePath );
            // FileSystemResource resource = new FileSystemResource(file);
            String wResponse = rest.reset()
                    .setApi( "files?workspace_name=" + ws.getName() )
                    .setRequestMethod( HttpMethod.POST )
                    // .setParameter("file", resource)
                    // .setParameter("description", desc.toString())
                    .setRequestHeaders( "description", desc.toString() )
                    .setInputStream( new FileInputStream( file ) )
                    .setResponseType( String.class ).exec().getBody()
                    .toString();
            fileId = JSON.parseObject( wResponse ).getJSONObject( "file" )
                    .getString( "id" );
        } catch ( HttpClientErrorException e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        // check
        String fileInfo = rest
                    .setApi( "files/id/" + fileId + "?workspace_name="
                            + ws.getName() )
                    .setRequestMethod( HttpMethod.HEAD ).exec().getHeaders()
                    .get( "file" ).toString();
            fileInfo = URLDecoder.decode( fileInfo, "UTF-8" );
            JSONObject fileInfo2JSON = JSON.parseObject(
                    fileInfo.substring( 1, fileInfo.length() - 1 ) );
            Assert.assertEquals( fileInfo2JSON.getString( "name" ),
                    desc.getString( "name" ) );
            Assert.assertEquals( fileInfo2JSON.getString( "author" ),
                    desc.getString( "author" ) );
            Assert.assertEquals( fileInfo2JSON.getString( "title" ),
                    desc.getString( "title" ) );
            Assert.assertEquals( fileInfo2JSON.getString( "mime_type" ),
                    desc.getString( "mime_type" ) );
            Assert.assertEquals( fileInfo2JSON.getIntValue( "size" ), fileSize );
    }

    private void readAndCheck() throws IOException {
        String downloadPath;
        OutputStream fileStream = null;
        InputStream in = null;
        try {
            downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                    TestTools.getMethodName(), Thread.currentThread().getId() );
            ResponseEntity< ? > resource = rest.reset()
                    .setApi( "files/" + fileId + "?workspace_name="
                            + ws.getName() )
                    .setRequestMethod( HttpMethod.GET )
                    .setResponseType( Resource.class ).exec();
            fileStream = new FileOutputStream( new File( downloadPath ) );
            Resource rs = ( Resource ) resource.getBody();
            if ( rs != null ) {
                in = rs.getInputStream();
                int len;
                byte[] buffer = new byte[ 1024 ];
                while ( ( len = in.read( buffer, 0, 1024 ) ) != -1 ) {
                    fileStream.write( buffer, 0, len );
                }
            }

            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ), "filePath = " + filePath
                            + ",downloadPath = " + downloadPath );
            SiteWrapper[] expSites = { ScmInfo.getRootSite() };
            ScmFileUtils.checkMetaAndData( ws, new ScmId( fileId ), expSites,
                    localPath, filePath );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        } finally {
            if ( fileStream != null ) {
                fileStream.close();
            }
            if ( in != null ) {
                in.close();
            }
        }
    }

    private void deleteAndCheck() throws Exception {
        rest.reset()
                .setApi( "files/" + fileId + "?workspace_name=" + ws.getName()
                        + "&is_physical=true" )
                .setRequestMethod( HttpMethod.DELETE )
                .setResponseType( String.class ).exec();
        try {
            SiteWrapper[] expSites = { ScmInfo.getRootSite() };
            ScmFileUtils.checkMetaAndData( ws, new ScmId( fileId ), expSites,
                    localPath, filePath );
            Assert.assertFalse( true,
                    "File is unExisted, except throw e, but success." );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getErrorCode(),
                    ScmError.FILE_NOT_FOUND.getErrorCode(), e.getMessage() );
        }
    }
}
