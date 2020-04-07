/**
 *
 */
package com.sequoiacm.rest;

import java.io.IOException;

import org.bson.util.JSON;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
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
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description UpdateVersionFile1716.java
 * @author luweikang
 * @date 2018年6月14日
 */
public class UpdateVersionFile1716 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;

    private String fileName = "fileVersion1716";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 200 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        BreakpointUtil.checkDBDataSource();
        site = ScmInfo.getBranchSite();
        wsp = ScmInfo.getWs();

        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );

        fileId = VersionUtils.createFileByStream( ws, fileName, filedata );
        VersionUtils.createBreakpointFileByStream( ws, fileName, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {

        restUpdateVersionFile();

        VersionUtils.checkFileVersion( ws, fileId, 1 );
        VersionUtils.CheckFileContentByStream( ws, fileName, 1, filedata );

        runSuccess = true;

    }

    @AfterClass()
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.BreakpointFile.deleteInstance( ws, fileName );
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
        try {
            rest.connect( site.getSiteServiceName(), TestScmBase.scmUserName,
                    TestScmBase.scmPassword );
            rest.setRequestMethod( HttpMethod.PUT )
                    .setApi( "/files/" + fileId + "?workspace_name=" +
                            ws.getName() )
                    .setParameter( "major_version", 1 )
                    .setParameter( "file_info",
                            JSON.parse( "{'author':'fileVersion1716_1'}" ) )
                    .setParameter( "breakpoint_file", fileName )
                    .setResponseType( String.class )
                    .exec().getHeaders().getFirst( "file_info" ).toString();
        } catch ( HttpClientErrorException e ) {
            Assert.assertEquals( e.getStatusCode().value(),
                    ScmError.HTTP_BAD_REQUEST.getErrorCode(), e.getMessage() );
        } finally {
            rest.disconnect();
        }
    }
}
