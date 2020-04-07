package com.sequoiacm.version;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:update Content of  the current scm file from siteA,than
 * download the updatefile from siteB
 * testlink-case:SCM-1650
 *
 * @author wuyan
 * @Date 2018.06.04
 * @version 1.00
 */

public class UpdateContent1650 extends TestScmBase {
    private static WsWrapper wsp = null;
    private final int branSitesNum = 2;
    private List< SiteWrapper > branSites = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;
    private ScmId fileId = null;

    private String fileName = "file1650";
    private int writeSize = 1024 * 500;
    private int updateSize = 1024 * 1024 * 2;
    private byte[] filedata = new byte[ writeSize ];
    private File localPath = null;
    private String filePath = null;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + updateSize + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, updateSize );

        branSites = ScmInfo.getBranchSites( branSitesNum );
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = TestScmTools.createSession( branSites.get( 1 ) );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        //write and updateContent from siteA
        fileId = VersionUtils.createFileByStream( wsA, fileName, filedata );
        VersionUtils.updateContentByFile( wsA, fileName, fileId, filePath );

        //download file from siteB and check result
        int currentVersion = 2;
        int historyVersion = 1;
        //check fileContent
        VersionUtils.CheckFileContentByStream( wsA, fileName, historyVersion,
                filedata );
        VersionUtils.CheckFileContentByFile( wsB, fileName, currentVersion,
                filePath, localPath );

        //check the sitelist/currentversion/size
        SiteWrapper[] expSiteList = { branSites.get( 0 ), branSites.get( 1 ),
                ScmInfo.getRootSite() };
        SiteWrapper[] expSiteListA = { branSites.get( 0 ) };
        VersionUtils.checkSite( wsA, fileId, currentVersion, expSiteList );
        VersionUtils.checkSite( wsA, fileId, historyVersion, expSiteListA );
        VersionUtils.checkFileCurrentVersion( wsA, fileId, currentVersion );
        VersionUtils.checkFileSize( wsA, fileId, currentVersion, updateSize );
        VersionUtils.checkFileSize( wsA, fileId, historyVersion, writeSize );

    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( wsB, fileId, true );
            TestTools.LocalFile.removeFile( localPath );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }
}