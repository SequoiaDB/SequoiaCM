/**
 *
 */
package com.sequoiacm.rest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description BreakpointFile1353.java 查询所有断点文件
 * @author luweikang
 * @date 2018年5月24日
 */
public class BreakpointFile1353 extends TestScmBase {

    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile1353";
    private int fileSize = 1024 * 1024 * 5;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {

        BreakpointUtil.createBreakpointFile( ws, filePath, fileName + "_1",
                1352 * 512, ScmChecksumType.CRC32 );
        BreakpointUtil.createBreakpointFile( ws, filePath, fileName + "_2",
                1353 * 1024, ScmChecksumType.CRC32 );
        BreakpointUtil.createBreakpointFile( ws, filePath, fileName + "_3",
                1353 * 2048, ScmChecksumType.CRC32 );

        // 查询upload_size等于于1353*1024的断点文件
        this.checkBreakpointFileByFilter( ws.getName(),
                "{'upload_size': 1385472}" );

        // 使用不存在的ws获取断点文件信息
        this.checkBreakpointFileError( "ws1353" );

        // 使用缺少ws参数的rest请求获取断点文件信息
        this.errorRequest();
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName + "_1" );
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName + "_2" );
            ScmFactory.BreakpointFile.deleteInstance( ws, fileName + "_3" );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkBreakpointFileError( String wsName ) throws Exception {
        RestWrapper rest = new RestWrapper();
        try {
            rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
            rest.setRequestMethod( HttpMethod.GET )
                    .setApi( "/breakpointfiles?workspace_name=" + wsName )
                    .exec();
            Assert.fail( "use error ws get info should error" );
        } catch ( HttpClientErrorException | HttpServerErrorException e ) {
            Assert.assertEquals( e.getStatusCode().value(),
                    ScmError.HTTP_UNAUTHORIZED.getErrorCode(), e.getMessage() );
        } finally {
            rest.disconnect();
        }
    }

    private void errorRequest() throws Exception {
        RestWrapper rest = new RestWrapper();
        try {
            rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
            rest.setRequestMethod( HttpMethod.GET ).setApi( "/breakpointfiles" )
                    .exec();
            Assert.fail( "use error option get info should error" );
        } catch ( HttpClientErrorException | HttpServerErrorException e ) {
            Assert.assertEquals( e.getStatusCode().value(),
                    ScmError.HTTP_BAD_REQUEST.getErrorCode(), e.getMessage() );
        } finally {
            rest.disconnect();
        }
    }

    @SuppressWarnings("unchecked")
    private void checkBreakpointFileByFilter( String wsName, String filter )
            throws Exception {
        RestWrapper rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        String response = rest.setRequestMethod( HttpMethod.GET )
                .setApi( "/breakpointfiles?workspace_name=" + wsName
                        + "&&filter={uri}" )
                .setUriVariables( new Object[] { filter } )
                .setResponseType( String.class ).exec().getBody().toString();
        ArrayList< BSONObject > objList = ( ArrayList< BSONObject > ) JSON
                .parse( response );
        rest.disconnect();
        Assert.assertEquals( objList.size(), 1, "get breakpointfile Info" );
    }
}
