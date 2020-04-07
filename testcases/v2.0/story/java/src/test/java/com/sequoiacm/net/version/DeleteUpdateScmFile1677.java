package com.sequoiacm.net.version;

import java.io.File;
import java.io.IOException;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType.ScopeType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmQueryBuilder;
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
 * test content: there are multiple versions of the file ,than delete the
 * scmfile
 * testlink-case:SCM-1677
 *
 * @author wuyan
 * @Date 2018.06.11
 * @version 1.00
 */

public class DeleteUpdateScmFile1677 extends TestScmBase {
    private static WsWrapper wsp = null;
    private SiteWrapper branSite = null;
    private SiteWrapper rootSite = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmId fileId = null;

    private String fileName = "file1677a";
    private int fileSize = 1024 * 800;
    private File localPath = null;
    private String filePath = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        branSite = ScmInfo.getBranchSite();
        rootSite = ScmInfo.getRootSite();
        wsp = ScmInfo.getWs();

        sessionA = TestScmTools.createSession( branSite );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionM = TestScmTools.createSession( rootSite );
        wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

        fileId = VersionUtils.createFileByFile( wsA, fileName, filePath );
    }

    @Test(groups = { "twoSite", "fourSite" })
    private void test() throws Exception {
        int updateTimes = 200;
        updateContextFile( wsA, updateTimes );
        ScmFactory.File.deleteInstance( wsA, fileId, true );
        checkDeleteResult( wsM );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
            if ( sessionM != null ) {
                sessionM.close();
            }
        }
    }

    private void updateContextFile( ScmWorkspace ws, int times )
            throws Exception {
        for ( int i = 0; i < times; i++ ) {
            byte[] updateData = new byte[ 1024 * i + 1024 ];
            VersionUtils.updateContentByStream( ws, fileId, updateData );
            int version = i + 2;
            VersionUtils.CheckFileContentByStream( ws, fileName, version,
                    updateData );
        }
    }

    private void checkDeleteResult( ScmWorkspace ws ) throws ScmException {
        BSONObject fileCond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        long currentCount = ScmFactory.File
                .countInstance( ws, ScopeType.SCOPE_CURRENT, fileCond );
        long historyCount = ScmFactory.File
                .countInstance( ws, ScopeType.SCOPE_HISTORY, fileCond );
        long expCount = 0;
        Assert.assertEquals( currentCount, expCount,
                " the currentVersion file must be delete" );
        Assert.assertEquals( historyCount, expCount,
                " the historyVersion file must be delete" );
    }
}