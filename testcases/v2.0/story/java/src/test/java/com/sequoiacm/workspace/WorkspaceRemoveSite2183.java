package com.sequoiacm.workspace;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @Description WorkspaceRemoveSite2183.java ws删除有数据的站点后,进行数据操作
 * @author luweikang
 * @date 2018年8月28日
 */
public class WorkspaceRemoveSite2183 extends TestScmBase {

    private static SiteWrapper siteM = null;
    private static SiteWrapper siteA = null;
    private ScmSession sessionM = null;
    private ScmSession sessionA = null;
    private String wsName = "ws2183";
    private String fileName = "file2183";
    private List< ScmId > idList = new ArrayList<>();

    @BeforeClass
    public void setUp() throws Exception {
        siteM = ScmInfo.getRootSite();
        siteA = ScmInfo.getBranchSite();
        sessionM = TestScmTools.createSession( siteM );
        sessionA = TestScmTools.createSession( siteA );
        ScmWorkspaceUtil.deleteWs( wsName, sessionM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws ScmException, InterruptedException, IOException {
        int siteNum = ScmInfo.getSiteNum();
        ScmWorkspace wsM = ScmWorkspaceUtil.createWS( sessionM, wsName,
                siteNum );
        ScmWorkspaceUtil.wsSetPriority( sessionM, wsName );
        wsUploadFile();
        ScmWorkspace wsA = ScmFactory.Workspace.getWorkspace( wsName,
                sessionA );
        ScmWorkspaceUtil.wsRemoveSite( wsM, siteA.getSiteName() );
        for ( int i = 0; i < idList.size(); i++ ) {
            ScmFactory.File.deleteInstance( wsM, idList.get( i ), true );
        }

        byte[] data = new byte[ 1024 ];
        new Random().nextBytes( data );
        InputStream is = new ByteArrayInputStream( data );
        try {
            ScmFile file = ScmFactory.File.createInstance( wsA );
            file.setFileName( "upload" + fileName );
            file.setContent( is );
            file.save();
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(),
                    ScmError.SERVER_NOT_IN_WORKSPACE );
        } finally {
            is.close();
        }
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            ScmWorkspaceUtil.deleteWs( wsName, sessionM );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void wsUploadFile() throws ScmException, IOException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, sessionM );
        int fileNum = 10;
        byte[] data = new byte[ 1024 ];
        new Random().nextBytes( data );
        for ( int i = 0; i < fileNum; i++ ) {
            InputStream is = new ByteArrayInputStream( data );
            try {
                ScmFile file = ScmFactory.File.createInstance( ws );
                file.setFileName( fileName + i );
                file.setContent( is );
                idList.add( file.save() );
            } finally {
                is.close();
            }
        }
    }
}
