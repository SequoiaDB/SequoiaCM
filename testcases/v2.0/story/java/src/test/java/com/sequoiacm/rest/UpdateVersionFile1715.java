package com.sequoiacm.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.breakpointfile.BreakpointUtil;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description updateVersionFile1715.java
 * @author luweikang
 * @date 2018年6月19日
 */
public class UpdateVersionFile1715 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;

    private int fileSize = 1024 * 200;
    private File localPath = null;
    private String filePath = null;
    private String fileName = "fileVersion1715";
    private byte[] filedata = new byte[ 1024 * 100 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();

        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        fileId = VersionUtils.createFileByStream( ws, fileName, filedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {

        restUpdateVersionFile();

        VersionUtils.CheckFileContentByFile( ws, fileId, 2, filePath,
                localPath );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void restUpdateVersionFile() throws Exception {

        RestWrapper rest = new RestWrapper();
        rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        rest.setRequestMethod( HttpMethod.PUT )
                .setApi( "/files/" + fileId + "?workspace_name=" + ws.getName()
                        + "&&major_version=1&&minor_version=0" )
                // .setParameter("file", new FileSystemResource(filePath))
                .setInputStream( new FileInputStream( new File( filePath ) ) )
                .setResponseType( String.class ).exec().getHeaders()
                .getFirst( "file_info" ).toString();
        rest.disconnect();

    }
}