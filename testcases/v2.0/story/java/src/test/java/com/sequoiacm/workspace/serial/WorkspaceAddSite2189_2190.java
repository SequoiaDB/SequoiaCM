package com.sequoiacm.workspace.serial;

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
 * @Description WorkspaceAddSite2189.java ws添加已有数据的站点 SCM-2190:ws添加站点
 * @author luweikang
 * @date 2018年8月28日
 */
public class WorkspaceAddSite2189_2190 extends TestScmBase {

    private ScmSession sessionM = null;
    private ScmSession sessionB = null;
    private SiteWrapper rootSite = null;
    private SiteWrapper branchSite = null;
    private String wsNameA = "ws2189_A";
    private String wsNameB = "ws2189_B";
    private String fileName = "file2189";
    private List< ScmId > idAList = new ArrayList<>();
    private List< ScmId > idBList = new ArrayList<>();
    private ScmWorkspace wsA = null;
    private ScmWorkspace wsB = null;

    @BeforeClass
    public void setUp() throws Exception {
        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();
        sessionM = TestScmTools.createSession( rootSite );
        sessionB = TestScmTools.createSession( branchSite );
        ScmWorkspaceUtil.deleteWs( wsNameA, sessionM );
        ScmWorkspaceUtil.deleteWs( wsNameB, sessionM );
    }

    @Test(groups = { "twoSite", "fourSite" })
    public void test() throws ScmException, InterruptedException, IOException {
        int siteNum = ScmInfo.getSiteNum();
        wsA = ScmWorkspaceUtil.createWS( sessionM, wsNameA, siteNum );
        wsB = ScmWorkspaceUtil.createWS( sessionM, wsNameB, siteNum );
        ScmWorkspaceUtil.wsSetPriority( sessionM, wsNameA );
        ScmWorkspaceUtil.wsSetPriority( sessionM, wsNameB );

        ScmWorkspaceUtil.wsRemoveSite( wsB, branchSite.getSiteName() );
        wsUploadFile( wsNameA, fileName + "A1_", idAList );
        ScmWorkspaceUtil.wsRemoveSite( wsA, branchSite.getSiteName() );
        ScmWorkspaceUtil.wsAddSite( wsB, branchSite );
        for ( int i = 0; i < idAList.size(); i++ ) {
            try {
                ScmFactory.File.deleteInstance( wsB, idAList.get( i ), true );
            } catch ( ScmException e ) {
                Assert.assertEquals( e.getError(), ScmError.FILE_NOT_FOUND );
            }
        }
        wsUploadFile( wsNameB, fileName + "B1_", idBList );
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            for ( int i = 0; i < idAList.size(); i++ ) {
                ScmFactory.File.deleteInstance( wsA, idAList.get( i ), true );
            }
            for ( int i = 0; i < idBList.size(); i++ ) {
                ScmFactory.File.deleteInstance( wsB, idBList.get( i ), true );
            }
            ScmWorkspaceUtil.deleteWs( wsNameA, sessionM );
            ScmWorkspaceUtil.deleteWs( wsNameB, sessionM );
        } finally {
            if ( sessionM != null ) {
                sessionM.close();
            }
            if ( sessionB != null ) {
                sessionB.close();
            }
        }
    }

    private void wsUploadFile( String wsName, String fileName,
            List< ScmId > idList ) throws ScmException, IOException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, sessionB );
        int fileNum = 5;
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
