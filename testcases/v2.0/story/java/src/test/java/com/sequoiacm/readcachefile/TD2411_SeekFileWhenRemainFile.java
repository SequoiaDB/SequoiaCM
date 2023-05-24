package com.sequoiacm.readcachefile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.bson.BSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testresource.SkipTestException;

/**
 * @Description: SCM-2411 :: 分站点存在残留大小一致的文件，通过seekable的方式跨中心读取文件
 * @author fanyu
 * @Date:2019年02月28日
 * @version:1.0
 */

public class TD2411_SeekFileWhenRemainFile extends TestScmBase {
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private String fileName = "file2411";
    private ScmId fileId = null;
    private int fileSize = 100;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws ScmException, IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        rootSite = ScmInfo.getRootSite();
        branSites = ScmInfo.getBranchSites( branSitesNum );
        if ( branSites.get( 1 )
                .getDataType() == ScmType.DatasourceType.CEPH_S3 ) {
            throw new SkipTestException(
                    "the behavior of cephS3 is inconsistent with other data sources" );
        }
        wsp = ScmInfo.getWs();
        sessionA = ScmSessionUtils.createSession( branSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.AUTHOR )
                .is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );
    }

    @Test(groups = { "fourSite", "net" }, enabled = false)
    public void nettest() throws Exception {
        // write from centerA
        fileId = ScmFileUtils.create( wsA, fileName, filePath );
        // remain file from centerB
        TestSdbTools.Lob.putLob( branSites.get( 1 ), wsp, fileId, filePath );
        // read from centerB
        this.seekFile( branSites.get( 1 ) );
        // check result
        SiteWrapper[] expSites = { branSites.get( 0 ), branSites.get( 1 ) };
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                filePath );
        runSuccess = true;
    }

    @Test(groups = { "fourSite", "star" }, enabled = false)
    public void startest() throws Exception {
        // write from centerA
        fileId = ScmFileUtils.create( wsA, fileName, filePath );
        // remain file from centerB
        TestSdbTools.Lob.putLob( branSites.get( 1 ), wsp, fileId, filePath );
        // read from centerB
        this.seekFile( branSites.get( 1 ) );
        // check result
        SiteWrapper[] expSites = { branSites.get( 0 ), branSites.get( 1 ) };
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                filePath );
        // read from centerM
        this.seekFile( rootSite );
        SiteWrapper[] expSites1 = { rootSite, branSites.get( 0 ),
                branSites.get( 1 ) };
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites1, localPath,
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

    private void seekFile( SiteWrapper site ) throws Exception {
        ScmSession session = null;
        OutputStream fos = null;
        ScmInputStream in = null;
        try {
            session = ScmSessionUtils.createSession( site );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );
            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            fos = new FileOutputStream( new File( downloadPath ) );
            in = ScmFactory.File.createInputStream(
                    ScmType.InputStreamType.SEEKABLE, scmfile );
            in.seek( CommonDefine.SeekType.SCM_FILE_SEEK_SET, 0 );
            in.read( fos );
        } finally {
            if ( fos != null )
                fos.close();
            if ( in != null )
                in.close();
            if ( session != null ) {
                session.close();
            }
        }
    }
}
