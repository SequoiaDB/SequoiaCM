package com.sequoiacm.readcachefile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
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
 * @Testcase: SCM-963:文件在A中心，B中心有残留相同LOB（大小和内容一致），B中心读取文件
 * @author huangxiaoni init
 * @date 2017.11.9
 */

public class TD963_AcrossCenterReadFileWhenRemainFile extends TestScmBase {
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = new ArrayList<>();
    private WsWrapper wsp = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;

    private String fileName = "file963";
    private ScmId fileId = null;
    private int fileSize1 = 250;
    private int fileSize2 = 255;
    private File localPath = null;
    private String filePath1 = null;
    private String filePath2 = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath1 = localPath + File.separator + "localFile1_" + fileSize1
                + ".txt";
        filePath2 = localPath + File.separator + "localFile2_" + fileSize2
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath1, fileSize1 );
        TestTools.LocalFile.createFile( filePath2, fileSize2 );

        rootSite = ScmInfo.getRootSite();
        branSites = ScmInfo.getBranchSites( branSitesNum );
        wsp = ScmInfo.getWs();
        sessionA = ScmSessionUtils.createSession( branSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
    }

    @DataProvider(name = "range-provider")
    public Object[] generateRangData() throws Exception {
        return new Object[] { filePath1, filePath2 };
    }

    @Test(groups = { "fourSite", "net" }, dataProvider = "range-provider")
    public void nettest( String filePath ) throws Exception {
        // write from centerA
        fileId = ScmFileUtils.create( wsA, fileName, filePath );

        // remain file from centerB
        TestSdbTools.Lob.putLob( branSites.get( 1 ), wsp, fileId, filePath );

        // read from centerB
        this.readFileFrom( branSites.get( 1 ), filePath );

        // check result
        SiteWrapper[] expSites = { branSites.get( 0 ), branSites.get( 1 ) };
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                filePath );
        ScmFileUtils.cleanFile( wsA, fileName );
        runSuccess = true;
    }

    @Test(groups = { "fourSite", "star" }, dataProvider = "range-provider")
    public void startest( String filePath ) throws Exception {
        // write from centerA
        fileId = ScmFileUtils.create( wsA, fileName, filePath );

        // remain file from centerB
        TestSdbTools.Lob.putLob( branSites.get( 1 ), wsp, fileId, filePath );

        // read from centerB
        this.readFileFrom( branSites.get( 1 ), filePath );

        // check result
        SiteWrapper[] expSites;
        if ( branSites.get( 1 ).getDataType() == ScmType.DatasourceType.CEPH_S3
                || branSites.get( 1 )
                        .getDataType() == ScmType.DatasourceType.SEQUOIADB ) {
            expSites = new SiteWrapper[] { branSites.get( 0 ),
                    branSites.get( 1 ), rootSite };
        } else {
            expSites = new SiteWrapper[] { branSites.get( 0 ),
                    branSites.get( 1 ) };
        }
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                filePath );

        // read from centerM
        this.readFileFrom( rootSite, filePath );
        // check result
        SiteWrapper[] expSites2 = { rootSite, branSites.get( 0 ),
                branSites.get( 1 ) };
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites2, localPath,
                filePath );
        ScmFileUtils.cleanFile( wsA, fileName );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess || forceClear ) {
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( sessionA != null ) {
                sessionA.close();
            }
        }
    }

    private void readFileFrom( SiteWrapper site, String filePath )
            throws Exception {
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
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }
}
