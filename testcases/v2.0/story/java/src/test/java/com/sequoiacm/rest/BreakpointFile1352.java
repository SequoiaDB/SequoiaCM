package com.sequoiacm.rest;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.bson.BSONObject;
import org.bson.util.JSON;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.checksum.ChecksumType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description BreakpointFile1352.java
 * @author luweikang
 * @date 2018年5月24日
 */
public class BreakpointFile1352 extends TestScmBase {

    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile1352";
    private int fileSize = 1024 * 1024 * 1;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {

        BreakpointUtil.createBreakpointFile( ws, filePath, fileName, 1352 * 512,
                ScmChecksumType.CRC32 );

        // 使用rest接口获取断点文件信息
        this.checkBreakpointFile( ws.getName(), fileName );

        // 使用不存在的ws获取断点文件信息
        this.checkBreakpointFileError( "ws1352", fileName, "nows" );

        // 使用不存在的文件名获取断点文件信息
        this.checkBreakpointFileError( ws.getName(), "nofile1352", "nofile" );

        // 使用缺少ws参数的rest请求获取断点文件信息
        this.errorRequest();
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkBreakpointFile( String wsName, String filename )
            throws Exception {
        RestWrapper rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        String response = rest.setRequestMethod( HttpMethod.HEAD )
                .setApi( "/breakpointfiles/" + filename + "?workspace_name="
                        + wsName )
                .setResponseType( String.class ).exec().getHeaders()
                .getFirst( "X-SCM-BREAKPOINTFILE" ).toString();
        BSONObject obj = ( BSONObject ) JSON
                .parse( URLDecoder.decode( response, "utf-8" ) );
        rest.disconnect();
        Assert.assertEquals( obj.get( "upload_size" ), 1352 * 512 );
        Assert.assertEquals( obj.get( "checksum_type" ),
                ChecksumType.CRC32.toString() );
    }

    private void checkBreakpointFileError( String wsName, String filename,
            String errorType ) throws Exception {
        RestWrapper rest = new RestWrapper();
        try {
            rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
            rest.setRequestMethod( HttpMethod.HEAD ).setApi( "/breakpointfiles/"
                    + filename + "?workspace_name=" + wsName ).exec();
            Assert.fail( "use error option get info should error" );
        } catch ( HttpClientErrorException | HttpServerErrorException e ) {
            if ( errorType.equals( "nofile" ) ) {
                Assert.assertEquals( e.getStatusCode().value(),
                        ScmError.HTTP_NOT_FOUND.getErrorCode(),
                        e.getMessage() );
            } else {
                Assert.assertEquals( e.getStatusCode().value(),
                        ScmError.HTTP_INTERNAL_SERVER_ERROR.getErrorCode(),
                        e.getMessage() );
            }
        } finally {
            rest.disconnect();
        }
    }

    private void errorRequest() throws Exception {
        RestWrapper rest = new RestWrapper();
        try {
            rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
            rest.setRequestMethod( HttpMethod.HEAD )
                    .setApi( "/breakpointfiles/" + fileName ).exec();
            Assert.fail( "use error option get info should error" );
        } catch ( HttpClientErrorException | HttpServerErrorException e ) {
            Assert.assertEquals( e.getStatusCode().value(),
                    ScmError.HTTP_BAD_REQUEST.getErrorCode(), e.getMessage() );
        } finally {
            rest.disconnect();
        }
    }
}
