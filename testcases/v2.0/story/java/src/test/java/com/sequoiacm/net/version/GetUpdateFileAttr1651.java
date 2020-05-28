package com.sequoiacm.net.version;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.VersionUtils;

/**
 * test content:write to a file , and updatecontent of the file, gets property
 * information for the specified version file testlink-case:SCM-1651
 *
 * @author wuyan
 * @Date 2018.06.04
 * @version 1.00
 */

public class GetUpdateFileAttr1651 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmId fileId = null;
    private String fileName = "file1651";
    private int writeSize = 1024 * 100;
    private int updateSize = 1024 * 200;
    private byte[] writeData = new byte[ writeSize ];
    private byte[] updateData = new byte[ updateSize ];

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        site = ScmInfo.getSite();
        wsp = ScmInfo.getWs();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() throws Exception {
        // write /update the file
        fileId = VersionUtils.createFileByStream( ws, fileName, writeData );
        VersionUtils.updateContentByStream( ws, fileId, updateData );

        // get the file attribute
        int currentVersion = 2;
        int historyVersion = 1;
        // test a: get history file attributes
        getHistoryFileAttr( historyVersion );
        // test b: get current file attributes
        getCurrentFileAttr( currentVersion );
        // test c: get file attributes,the version is not exist
        getVersionNoExist();

    }

    @AfterClass
    private void tearDown() {
        try {
            ScmFactory.File.deleteInstance( ws, fileId, true );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void getCurrentFileAttr( int currentVersion ) throws ScmException {

        // get site
        SiteWrapper[] expSiteList = { site };
        VersionUtils.checkSite( ws, fileId, currentVersion, expSiteList );

        // get filename/user/size/version/ws
        ScmFile file = ScmFactory.File.getInstanceByPath( ws, fileName,
                currentVersion, 0 );
        Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
        Assert.assertEquals( file.getUpdateUser(), TestScmBase.scmUserName );
        Assert.assertEquals( file.getFileName(), fileName );
        Assert.assertEquals( file.getMajorVersion(), currentVersion );
        Assert.assertEquals( file.getWorkspaceName(), ws.getName() );
        Assert.assertEquals( file.getSize(), updateSize );

    }

    private void getHistoryFileAttr( int historyVersion ) throws ScmException {
        // get site
        SiteWrapper[] expSiteList = { site };
        VersionUtils.checkSite( ws, fileId, historyVersion, expSiteList );

        // get filename/user/size/version/ws
        ScmFile file = ScmFactory.File.getInstanceByPath( ws, fileName,
                historyVersion, 0 );
        Assert.assertEquals( file.getUser(), TestScmBase.scmUserName );
        Assert.assertEquals( file.getUpdateUser(), TestScmBase.scmUserName );
        Assert.assertEquals( file.getFileName(), fileName );
        Assert.assertEquals( file.getMajorVersion(), historyVersion );
        Assert.assertEquals( file.getWorkspaceName(), ws.getName() );
        Assert.assertEquals( file.getSize(), writeSize );
    }

    private void getVersionNoExist() {
        int version = 4;
        try {
            ScmFactory.File.getInstanceByPath( ws, fileName, version, 0 );
            Assert.fail( "get  file must bu fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.FILE_NOT_FOUND != e.getError() ) {
                Assert.fail( "expErrorCode:-262  actError:" + e.getError()
                        + e.getMessage() );
            }
        }
    }
}