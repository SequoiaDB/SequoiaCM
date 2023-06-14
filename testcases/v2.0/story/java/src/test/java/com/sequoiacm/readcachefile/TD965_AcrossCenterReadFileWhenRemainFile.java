package com.sequoiacm.readcachefile;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-965:文件在A中心，B中心有残留相同LOB（大小一致内容不一致），B中心读取文件
 * @author huangxiaoni init
 * @date 2017.11.9
 */

public class TD965_AcrossCenterReadFileWhenRemainFile extends TestScmBase {
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;

    private String fileName = "file965";
    private ScmId fileId = null;
    private int fileSize = 10;
    private File localPath = null;
    private String filePath = null;
    private String remainFilePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        remainFilePath = localPath + File.separator + "localFile_" + fileSize
                + "_2.txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, "test", fileSize );
        TestTools.LocalFile.createFile( remainFilePath, "hello", fileSize );

        rootSite = ScmInfo.getRootSite();
        branSites = ScmInfo.getBranchSites( branSitesNum );

        wsp = ScmInfo.getWs();

        sessionA = ScmSessionUtils.createSession( branSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
    }

    @Test(groups = { "fourSite", "net" })
    public void nettest() throws Exception {
        // write from centerA
        fileId = ScmFileUtils.create( wsA, fileName, filePath );
        // remain file from centerB
        TestSdbTools.Lob.putLob( branSites.get( 1 ), wsp, fileId,
                remainFilePath );
        // read from centerB
        this.readFileFrom( branSites.get( 1 ) );
        // check meta,because the metadata is directly modified when
        // remainsize is equal to filesize,
        // rootsite does not cache data
        SiteWrapper[] expSites = { branSites.get( 0 ), branSites.get( 1 ) };
        ScmFileUtils.checkMeta( wsA, fileId, expSites );
        ScmFileUtils.checkData( wsA, fileId, localPath, filePath );

        // check contents
        ScmSession sessionM = null;
        ScmSession sessionB = null;
        try {
            sessionB = ScmSessionUtils.createSession( branSites.get( 1 ) );
            ScmWorkspace wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    sessionB );
            ScmFileUtils.checkData( wsB, fileId, localPath, remainFilePath );
        } finally {
            if ( null != sessionM ) {
                sessionM.close();
            }
            if ( null != sessionB ) {
                sessionB.close();
            }
        }
        runSuccess = true;
    }

    @Test(groups = { "fourSite", "star" })
    public void startest() throws Exception {
        // write from centerA
        fileId = ScmFileUtils.create( wsA, fileName, filePath );
        // remain file from centerB
        TestSdbTools.Lob.putLob( rootSite, wsp, fileId, remainFilePath );
        TestSdbTools.Lob.putLob( branSites.get( 1 ), wsp, fileId,
                remainFilePath );
        // read from centerB
        this.readFileFrom( branSites.get( 1 ) );
        // check meta,because the metadata is directly modified when
        // remainsize is equal to filesize,
        // rootsite does not cache data
        SiteWrapper[] expSites;
        if ( branSites.get( 1 )
                .getDataType() == ScmType.DatasourceType.CEPH_S3 ) {
            expSites = new SiteWrapper[] { branSites.get( 0 ),
                    branSites.get( 1 ), rootSite };
        } else {
            expSites = new SiteWrapper[] { branSites.get( 0 ),
                    branSites.get( 1 ) };
        }
        ScmFileUtils.checkMeta( wsA, fileId, expSites );
        // read from centerM
        this.readFileFrom( rootSite );
        // check meta
        SiteWrapper[] expSites2 = { rootSite, branSites.get( 0 ),
                branSites.get( 1 ) };
        ScmFileUtils.checkMeta( wsA, fileId, expSites2 );

        // check contents
        ScmSession sessionM = null;
        ScmSession sessionB = null;
        try {
            sessionM = ScmSessionUtils.createSession( rootSite );
            ScmWorkspace wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    sessionM );
            ScmFileUtils.checkData( wsM, fileId, localPath, remainFilePath );

            sessionB = ScmSessionUtils.createSession( branSites.get( 1 ) );
            ScmWorkspace wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    sessionB );
            ScmFileUtils.checkData( wsB, fileId, localPath, remainFilePath );
        } finally {
            if ( null != sessionM ) {
                sessionM.close();
            }
            if ( null != sessionB ) {
                sessionB.close();
            }
        }
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void readFileFrom( SiteWrapper site ) throws Exception {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );

            // read scmfile
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            file.getContent( downloadPath );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
