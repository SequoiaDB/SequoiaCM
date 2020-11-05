package com.sequoiacm.rest;

import java.io.File;
import java.io.IOException;

import org.bson.BasicBSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.common.ScmChecksumType;
import com.sequoiacm.client.core.ScmBreakpointFile;
import com.sequoiacm.client.core.ScmCursor;
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
 * @Description BreakpointFile1392.java 指定ws和文件不存在，执行删除操作
 * @author luweikang
 * @date 2018年5月22日
 */
public class BreakpointFile1392 extends TestScmBase {

    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile1392";
    private int fileSize = 1024 * 1024;
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

        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        try {
            BreakpointUtil.createBreakpointFile( ws, filePath, fileName,
                    1024 * 512, ScmChecksumType.ADLER32 );
        } catch ( ScmException e ) {
            e.printStackTrace();
        }
        // 指定文件不存在
        this.deleteBreakpointFile( ws.getName(), "nofile1392", "nofile" );
        // 指定ws不存在
        this.deleteBreakpointFile( "ws1392", fileName, "nows" );
        // 检查结果
        this.checkBreakpointFile();
        // 删除文件成功
        this.deleteBreakpointFile();
    }

    @AfterClass
    private void tearDown() {
        try {
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void deleteBreakpointFile( String wsName, String filename,
            String errorType ) throws Exception {
        RestWrapper rest = new RestWrapper();
        try {
            rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
            rest.setRequestMethod( HttpMethod.DELETE )
                    .setApi( "/breakpointfiles/" + filename + "?workspace_name="
                            + wsName )
                    .exec();
            Assert.fail( "delete breakpointFile should error" );
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

    private void checkBreakpointFile() throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .getInstance( ws, fileName );
        Assert.assertEquals( breakpointFile.isCompleted(), false,
                "check breakpointFile isCompleted" );
        Assert.assertEquals( breakpointFile.getUploadSize(), 1024 * 512,
                "check breakpointFile uploadSize" );
    }

    private void deleteBreakpointFile() throws Exception {
        RestWrapper rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        rest.setRequestMethod( HttpMethod.DELETE ).setApi( "/breakpointfiles/"
                + fileName + "?workspace_name=" + ws.getName() ).exec();
        ScmCursor< ScmBreakpointFile > cursor = ScmFactory.BreakpointFile
                .listInstance( ws,
                        new BasicBSONObject( "file_name", fileName ) );
        rest.disconnect();
        Assert.assertEquals( cursor.getNext(), null,
                "checkBreakpointFile should null" );
    }
}
