package com.sequoiacm.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmBreakpointFileUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.util.JSON;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.checksum.ChecksumType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;

/**
 * @Description BreakpointFile1356.java 上传断点文件
 * @author luweikang
 * @date 2018年5月24日
 */
public class BreakpointFile1356 extends TestScmBase {

    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "scmfile1356";
    private int fileSize = 1024 * 1024 * 1;
    private File localPath = null;
    private String filePath = null;
    private String checkFilePath = null;
    private String fileId = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        List< SiteWrapper > sites = ScmBreakpointFileUtils.checkDBAndCephS3DataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        checkFilePath = localPath + File.separator + "localFile_check"
                + fileSize + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        BreakpointUtil.createFile( filePath, fileSize );

        site = sites.get( new Random().nextInt( sites.size() ) );
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { GroupTags.base })
    private void test() throws Exception {

        // 创建空的断点文件
        this.createBreakpointFile();

        // 指定错误的offset
        this.uploadBreakpointFileFail();

        // 续传断点文件
        this.uploadBreakpointFile();

        // 断点文件转化为SCM文件
        this.breakpointFile2ScmFile();

        // 检查文件
        this.checkScmFile();

        // 使用缺少ws参数的rest请求获取断点文件信息
        this.errorRequest();
    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( ws, new ScmId( fileId ), true );
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.removeFile( checkFilePath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createBreakpointFile() throws Exception {
        RestWrapper rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        rest.setRequestMethod( HttpMethod.POST )
                .setApi( "/breakpointfiles/" + fileName + "?workspace_name="
                        + ws.getName() )
                .setParameter( "checksum_type", ChecksumType.ADLER32.name() )
                .setParameter( "is_last_content", false ).exec();
        rest.disconnect();
    }

    private void uploadBreakpointFile() throws Exception {
        RestWrapper rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        rest.setRequestMethod( HttpMethod.PUT )
                .setApi( "/breakpointfiles/" + fileName + "?workspace_name="
                        + ws.getName() + "&&offset=0&&is_last_content=true" )
                .setInputStream( new FileInputStream( new File( filePath ) ) )
                .exec();
        rest.disconnect();
    }

    private void errorRequest() throws Exception {
        RestWrapper rest = new RestWrapper();
        try {
            rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
            rest.setRequestMethod( HttpMethod.POST )
                    .setApi( "/breakpointfiles/" + fileName )
                    .setParameter( "checksum_type",
                            ChecksumType.ADLER32.name() )
                    .setParameter( "is_last_content", false ).exec();
            Assert.fail(
                    "use error option create breakpointFile should error" );
        } catch ( HttpClientErrorException | HttpServerErrorException e ) {
            Assert.assertEquals( e.getStatusCode().value(),
                    ScmError.HTTP_BAD_REQUEST.getErrorCode(), e.getMessage() );
        } finally {
            rest.disconnect();
        }
    }

    private void uploadBreakpointFileFail() throws Exception {
        RestWrapper rest = new RestWrapper();
        try {
            rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
            rest.setRequestMethod( HttpMethod.PUT )
                    .setApi( "/breakpointfiles/" + fileName + "?workspace_name="
                            + ws.getName()
                            + "&&offset=524288&&is_last_content=true" )
                    // .setParameter("file", new FileSystemResource(filePath))
                    .setInputStream(
                            new FileInputStream( new File( filePath ) ) )
                    .exec();
        } catch ( HttpClientErrorException | HttpServerErrorException e ) {
            Assert.assertEquals( e.getStatusCode().value(),
                    ScmError.HTTP_BAD_REQUEST.getErrorCode(), e.getMessage() );
        } finally {
            rest.disconnect();
        }
    }

    private void breakpointFile2ScmFile() throws Exception {
        RestWrapper rest = new RestWrapper();
        BSONObject description = new BasicBSONObject();
        description.put( "name", fileName );
        description.put( "title", fileName );
        description.put( "mime_type", "scmfile1356.txt" );
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        String response = rest.setRequestMethod( HttpMethod.POST )
                .setApi( "/files?workspace_name=" + ws.getName() )
                // .setRequestHeaders("description", description.toString())
                // .setInputStream(new FileInputStream(fileName))
                .setParameter( "breakpoint_file", fileName )
                // .setParameter("description", description.toString())
                // .setRequestHeaders("breakpoint_file", fileName)
                .setRequestHeaders( "description", description.toString() )
                .setResponseType( String.class ).exec().getBody().toString();
        BSONObject obj = ( BSONObject ) JSON.parse( response );
        BSONObject file = ( BSONObject ) obj.get( "file" );
        fileId = ( String ) file.get( "id" );
        rest.disconnect();
    }

    private void checkScmFile() throws ScmException, IOException {
        ScmFile scmFile = ScmFactory.File.getInstance( ws,
                new ScmId( fileId ) );
        scmFile.getContent( checkFilePath );
        Assert.assertEquals( TestTools.getMD5( checkFilePath ),
                TestTools.getMD5( filePath ),
                "check breakpointFile to ScmFile" );
    }
}
