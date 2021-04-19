package com.sequoiacm.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
 * @Description:TODO
 * @author fanyu
 * @Date:2018年3月23日
 * @version:1.0
 */
public class WRDMutilEncodeFile1110 extends TestScmBase {
    private boolean runSuccess = false;
    private WsWrapper ws = null;
    private File localPath = null;
    private List< String > fileIdList = new ArrayList< String >();
    private int fileSize = 1024 * 100;
    // private String author = "WRDMutilEncodeFile1110";
    private RestWrapper rest = null;
    private String[] charset = { "utf-8", "GBK", "GB2312" };
    private String[] filePaths = new String[ 3 ];
    private SiteWrapper site = null;

    public static void createFile( String filePath, int size, String Encoding )
            throws IOException {
        OutputStreamWriter fos = null;
        try {
            TestTools.LocalFile.createFile( filePath );
            fos = new OutputStreamWriter( new FileOutputStream( filePath ),
                    Encoding );
            int written = 0;
            byte[] fileBlock = new byte[ 1024 ];
            new Random().nextBytes( fileBlock );
            String fileBlock1 = new String( fileBlock );
            while ( written < size ) {
                int toWrite = size - written;
                int len = fileBlock1.length() < toWrite ? fileBlock1.length()
                        : toWrite;
                fos.write( fileBlock1.toCharArray(), 0, len );
                written += len;
            }
        } catch ( IOException e ) {
            throw e;
        } finally {
            if ( fos != null ) {
                fos.close();
            }
        }
    }

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        try {
            localPath = new File( TestScmBase.dataDirectory + File.separator
                    + TestTools.getClassName() );
            // ready file
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            for ( int i = 0; i < charset.length; i++ ) {
                String filePath = localPath + File.separator + "localFile_"
                        + fileSize + charset[ i ] + ".txt";
                createFile( filePath, fileSize, charset[ i ] );
                filePaths[ i ] = filePath;
            }
            site = ScmInfo.getRootSite();
            ws = ScmInfo.getWs();
            rest = new RestWrapper();
            rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        for ( int i = 0; i < charset.length; i++ ) {
            writeAndCheck( filePaths[ i ] );
            readAndCheck( fileIdList.get( i ), filePaths[ i ] );
            deleteAndCheck( fileIdList.get( i ), filePaths[ i ] );
        }
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

    private void writeAndCheck( String filePath )
            throws UnsupportedEncodingException, FileNotFoundException {
        JSONObject desc = null;
        String fileId = null;
        // write
        try {
            desc = new JSONObject();
            desc.put( "name",
                    this.getClass().getSimpleName() + UUID.randomUUID() );
            desc.put( "author", this.getClass().getSimpleName() );
            desc.put( "title", this.getClass().getSimpleName() );
            desc.put( "mime_type", "text/plain" );
            File file = new File( filePath );
            // FileSystemResource resource = new FileSystemResource(file);
            String wResponse = rest
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
            fileIdList.add( fileId );
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
        JSONObject fileInfo2JSON = JSON
                .parseObject( fileInfo.substring( 1, fileInfo.length() - 1 ) );
        Assert.assertEquals( fileInfo2JSON.getString( "name" ),
                desc.getString( "name" ) );
        Assert.assertEquals( fileInfo2JSON.getString( "author" ),
                desc.getString( "author" ) );
        Assert.assertEquals( fileInfo2JSON.getString( "title" ),
                desc.getString( "title" ) );
        Assert.assertEquals( fileInfo2JSON.getString( "mime_type" ),
                desc.getString( "mime_type" ) );
    }

    private void readAndCheck( String fileId, String filePath )
            throws IOException {
        String downloadPath;
        OutputStream fileStream = null;
        InputStream in = null;
        try {
            downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                    TestTools.getMethodName(), Thread.currentThread().getId() );
            ResponseEntity< ? > resource = rest
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

    private void deleteAndCheck( String fileId, String filePath )
            throws Exception {
        rest.setApi( "files/" + fileId + "?workspace_name=" + ws.getName()
                + "&is_physical=true" ).setRequestMethod( HttpMethod.DELETE )
                .setResponseType( Resource.class ).exec();
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
