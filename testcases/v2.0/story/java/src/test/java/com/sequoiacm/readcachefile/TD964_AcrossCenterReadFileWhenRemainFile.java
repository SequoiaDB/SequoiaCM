package com.sequoiacm.readcachefile;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testresource.SkipTestException;

/**
 * @Testcase: SCM-964:文件在A中心，B中心有残留相同LOB（大小不一致），B中心读取文件
 * @author huangxiaoni init
 * @date 2017.11.9
 */

public class TD964_AcrossCenterReadFileWhenRemainFile extends TestScmBase {
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;

    private String fileName = "file964";
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
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( remainFilePath, fileSize - 1 );

        rootSite = ScmInfo.getRootSite();
        branSites = ScmInfo.getBranchSites( branSitesNum );
        if ( branSites.get( 1 )
                .getDataType() == ScmType.DatasourceType.CEPH_S3 ) {
            throw new SkipTestException( "源站点不能为ceph S3数据源" );
        }
        wsp = ScmInfo.getWs();

        sessionA = ScmSessionUtils.createSession( branSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { "fourSite", "net", GroupTags.base })
    public void nettest() throws Exception {
        // write from centerA
        fileId = ScmFileUtils.create( wsA, fileName, filePath );
        // remain file from centerB
        SiteWrapper[] expSites = new SiteWrapper[] { branSites.get( 0 ),
                branSites.get( 1 ) };
        TestSdbTools.Lob.putLob( branSites.get( 1 ), wsp, fileId,
                remainFilePath );
        // read from centerB
        this.readFileFromB();
        // check result

        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                filePath );
        runSuccess = true;
    }

    @Test(groups = { "fourSite", "star", GroupTags.base })
    public void startest() throws Exception {
        // write from centerA
        fileId = ScmFileUtils.create( wsA, fileName, filePath );
        // remain file from centerB
        TestSdbTools.Lob.putLob( rootSite, wsp, fileId, remainFilePath );
        SiteWrapper[] expSites = new SiteWrapper[] { rootSite,
                branSites.get( 0 ), branSites.get( 1 ) };
        TestSdbTools.Lob.putLob( branSites.get( 1 ), wsp, fileId,
                remainFilePath );
        // read from centerB
        this.readFileFromB();
        // check result

        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                filePath );
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

    private void readFileFromB() throws Exception {
        ScmSession session = null;
        try {
            session = ScmSessionUtils.createSession( branSites.get( 1 ) );
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
