/**
 *
 */
package com.sequoiacm.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.bson.BSONObject;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Description:SCM-2123 :跨中心偏移读取文件，指定off和length,其中length = -1（自offset起到文件末尾的长度）
 * @author wuyan
 * @Date: 2018.7.23
 * @version:1.0
 */
public class SeekReadFile2123 extends TestScmBase {
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private WsWrapper wsp = null;
    private File localPath = null;
    private String fileId = null;
    private String filePath = null;
    private int fileSize = 1024 * 1024;
    private String author = "seekReadFile2123";
    private String sessionId = null;
    private RestWrapper restA = null;
    private RestWrapper restB = null;
    private List< SiteWrapper > branSites = null;

    @BeforeClass()
    private void setUp() throws IOException,ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        // ready file
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        branSites = ScmInfo.getBranchSites( branSitesNum );
        wsp = ScmInfo.getWs();

        // clean file
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( author ).get();
        ScmFileUtils.cleanFile( wsp, cond );
        restA = new RestWrapper();
        restA.connect( branSites.get( 0 ).getSiteServiceName(),
                TestScmBase.scmUserName, TestScmBase.scmPassword );
        restB = new RestWrapper();
        restB.connect( branSites.get( 1 ).getSiteServiceName(),
                TestScmBase.scmUserName, TestScmBase.scmPassword );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        writeFile( restA );
        seekReadAndCheck( restB );
        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                deleteFile( restA );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( restA != null ) {
                restA.disconnect();
            }
            if ( restB != null ) {
                restB.disconnect();
            }
        }
    }

    private void writeFile( RestWrapper rest ) throws UnsupportedEncodingException, FileNotFoundException {
        // write
        JSONObject desc = new JSONObject();
        desc.put( "name", author );
        desc.put( "author", author );
        desc.put( "mime_type", "text/plain" );
        desc.put( "title", author );
        File file = new File( filePath );
        // FileSystemResource resource = new FileSystemResource(file);
        String wResponse = rest
                .setApi( "files?workspace_name=" + wsp.getName() )
                .setRequestMethod( HttpMethod.POST )
                // .setParameter("file", resource)
                // .setParameter("description", desc.toString())
                .setRequestHeaders( "description", desc.toString() )
                .setInputStream( new FileInputStream( file ) )
                .setResponseType( String.class ).exec().getBody().toString();
        fileId = JSON.parseObject( wResponse ).getJSONObject( "file" )
                .getString( "id" );
    }

    private void seekReadAndCheck( RestWrapper rest ) throws Exception {
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        OutputStream fileStream = new FileOutputStream(
                new File( downloadPath ) );
        InputStream in = null;
        long offset = 1;
        long length = -1;
        try {
            // seek read
            ResponseEntity< ? > resource = rest
                    .setApi( "files/" + fileId + "?workspace_name="
                            + wsp.getName() + "&offset=" + offset + "&length="
                            + length )
                    .setRequestMethod( HttpMethod.GET )
                    .setResponseType( Resource.class ).exec();
            HttpHeaders hs = resource.getHeaders();
            int curReadSize = Integer
                    .parseInt( hs.get( "data_length" ).get( 0 ) );
            Assert.assertEquals( curReadSize, fileSize - offset );

            Resource rs = ( Resource ) resource.getBody();
            if ( rs != null ) {
                in = rs.getInputStream();
                int actReadSize;
                byte[] buffer = new byte[ ( int ) ( fileSize - offset ) ];
                while ( ( actReadSize = in.read( buffer, 0,
                        curReadSize ) ) != -1 ) {
                    fileStream.write( buffer, 0, actReadSize );
                }
            }

            // check results
            String tmpPath = TestTools.LocalFile.initDownloadPath( localPath,
                    TestTools.getMethodName(), Thread.currentThread().getId() );
            TestTools.LocalFile.readFile( filePath, ( int ) offset,
                    ( int ) ( fileSize - offset ), tmpPath );
            Assert.assertEquals( TestTools.getMD5( tmpPath ),
                    TestTools.getMD5( downloadPath ), "tmpPath = " + tmpPath
                            + ",downloadPath = " + downloadPath );
        } finally {
            if ( fileStream != null ) {
                fileStream.close();
            }
            if ( in != null ) {
                in.close();
            }
        }
    }

    private void deleteFile( RestWrapper rest ) throws Exception {
        rest.setApi( "files/" + fileId + "?workspace_name=" + wsp.getName()
                + "&is_physical=true" ).setRequestMethod( HttpMethod.DELETE )
                .setRequestHeaders( "Authorization", "Scm " + sessionId )
                .setResponseType( Resource.class ).exec();
    }
}
