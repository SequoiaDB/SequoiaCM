/**
 *
 */
package com.sequoiacm.rest;

import java.io.IOException;

import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.springframework.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.RestWrapper;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * @Description AsyncCacheVersionFile1718.java
 * @author luweikang
 * @date 2018年6月14日
 */
public class AsyncCacheVersionFile1718 extends TestScmBase {
    private static WsWrapper wsp = null;
    private boolean runSuccess = false;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;

    private String fileName = "fileVersion1718";
    private byte[] filedata = new byte[ 1024 * 100 ];
    private byte[] updatedata = new byte[ 1024 * 1024 * 2 ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = ScmSessionUtils.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = ScmSessionUtils.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        fileId = ScmFileUtils.createFileByStream( wsM, fileName, filedata );
        VersionUtils.updateContentByStream( wsM, fileId, updatedata );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {

        restUpdateVersionFile();

        VersionUtils.checkFileVersion( wsA, fileId, 1 );
        VersionUtils.CheckFileContentByStream( wsA, fileName, 1, filedata );

        runSuccess = true;

    }

    @AfterClass()
    private void tearDown() {
        try {
            if ( runSuccess ) {
                ScmFactory.File.deleteInstance( wsM, fileId, true );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() + e.getStackTrace() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void restUpdateVersionFile() throws Exception {

        RestWrapper rest = new RestWrapper();
        rest.connect( branSite.getSiteServiceName(), TestScmBase.scmUserName,
                TestScmBase.scmPassword );
        rest.setRequestMethod( HttpMethod.POST )
                .setApi( "/files/" + fileId + "/async-cache?workspace_name="
                        + wsA.getName() )
                .setParameter( "major_version", 1 )
                .setParameter( "minor_version", 0 ).exec();
        rest.disconnect();
    }
}
