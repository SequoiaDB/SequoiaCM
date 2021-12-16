package com.sequoiacm.readcachefile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmFileLocation;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestSdbTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase:SCM233 主中心跟A中心存在文件，B中心读取文件
 * @author fanyu init in 2017.05.10; huangxiaoni modify in 2017.6.6 linsuqiang
 *         modify in 2017.8.7
 */

/*
 * 1、在分中心A写入文件； 2、在主中心读取该文件； 3、检查主中心读取的文件正确性； 检查主中心该文件元数据信息正确性；
 * 检查主中心sdb中lob文件正确性； 4、在A中心sdb删除该lob文件； 5、在分中心B读取该文件； 6、检查分中心B读取的文件正确性；
 * 并检查主中心该文件元数据信息正确性； 7、检查元数据所有站点列表最近访问时间正确性；
 */

public class AcrossCenterReadFileByStream233 extends TestScmBase {
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;

    private ScmSession sessionM = null;
    private ScmWorkspace wsM = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;

    private String fileName = "readCacheFile233";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 2;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            rootSite = ScmInfo.getRootSite();
            branSites = ScmInfo.getBranchSites( branSitesNum );
            wsp = ScmInfo.getWs();

            BSONObject cond = ScmQueryBuilder
                    .start( ScmAttributeName.File.FILE_NAME ).is( fileName )
                    .get();
            ScmFileUtils.cleanFile( wsp, cond );

            sessionM = TestScmTools.createSession( rootSite );
            wsM = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionM );

            sessionA = TestScmTools.createSession( branSites.get( 0 ) );
            wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );

            sessionB = TestScmTools.createSession( branSites.get( 1 ) );
            wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite", "star" }) // bug:315
    private void test() throws Exception {
        fileId = ScmFileUtils.create( wsA, fileName, filePath );

        this.readFileFromM();

        TestSdbTools.Lob.removeLob( branSites.get( 0 ), wsp, fileId );
        this.readFileFromB();
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( wsM, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( sessionM != null )
                sessionM.close();
            if ( sessionA != null )
                sessionA.close();
            if ( sessionB != null )
                sessionB.close();

        }
    }

    private void readFileFromM() throws Exception {
        ScmFile file;

        // get lastAccessTime before read
        file = ScmFactory.File.getInstance( wsM, fileId );
        List< ScmFileLocation > preSiteInfoList = file.getLocationList();

        this.readFile( wsM );

        // get lastAccessTime after read
        file = ScmFactory.File.getInstance( wsM, fileId );
        List< ScmFileLocation > aftSiteInfoList = file.getLocationList();

        // check mete and data
        SiteWrapper[] expSites = { rootSite, branSites.get( 0 ) };
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                filePath );

        // check siteA'lastAccessTime, siteM'time is newly generated
        long preTimeA = 0;
        for ( int i = 0; i < preSiteInfoList.size(); i++ ) {
            if ( preSiteInfoList.get( i ).getSiteId() == branSites.get( 0 )
                    .getSiteId() ) {
                preTimeA = preSiteInfoList.get( i ).getDate().getTime();
            }
        }
        long aftTimeA = 0;
        for ( int i = 0; i < aftSiteInfoList.size(); i++ ) {
            if ( aftSiteInfoList.get( i ).getSiteId() == branSites.get( 0 )
                    .getSiteId() ) {
                aftTimeA = aftSiteInfoList.get( i ).getDate().getTime();
            }
        }
        if ( !( preTimeA < aftTimeA ) ) {
            Assert.fail( "failed to check lastAccessTime, preTimeA=" + preTimeA
                    + ", aftTimeA=" + aftTimeA );
        }
    }

    private void readFileFromB() throws Exception {
        ScmFile file;

        // get lastAccessTime before read
        file = ScmFactory.File.getInstance( wsM, fileId );
        List< ScmFileLocation > preSiteInfoList = file.getLocationList();

        this.readFile( wsB );

        // get lastAccessTime after read
        file = ScmFactory.File.getInstance( wsM, fileId );
        List< ScmFileLocation > aftSiteInfoList = file.getLocationList();

        // check mete and data
        SiteWrapper[] expSites = { rootSite, branSites.get( 0 ),
                branSites.get( 1 ) };
        ScmFileUtils.checkMeta( wsM, fileId, expSites );
        ScmFileUtils.checkData( wsM, fileId, localPath, filePath );
        ScmFileUtils.checkData( wsB, fileId, localPath, filePath );
        try {
            ScmFileUtils.checkData( wsA, fileId, localPath, filePath );
            Assert.fail(
                    "site'A Lob has be removed, expect fail, actual success." );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.DATA_NOT_EXIST ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        }

        // check siteA/siteM'lastAccessTime, siteB'time is newly generated
        long preTimeA = 0;
        long preTimeM = 0;
        for ( int i = 0; i < preSiteInfoList.size(); i++ ) {
            if ( preSiteInfoList.get( i ).getSiteId() == branSites.get( 0 )
                    .getSiteId() ) {
                preTimeA = preSiteInfoList.get( i ).getDate().getTime();
            }
            if ( preSiteInfoList.get( i ).getSiteId() == rootSite
                    .getSiteId() ) {
                preTimeM = preSiteInfoList.get( i ).getDate().getTime();
            }
        }
        long aftTimeA = 0;
        long aftTimeM = 0;
        for ( int i = 0; i < aftSiteInfoList.size(); i++ ) {
            if ( aftSiteInfoList.get( i ).getSiteId() == branSites.get( 0 )
                    .getSiteId() ) {
                aftTimeA = aftSiteInfoList.get( i ).getDate().getTime();
            }
            if ( aftSiteInfoList.get( i ).getSiteId() == rootSite
                    .getSiteId() ) {
                aftTimeM = aftSiteInfoList.get( i ).getDate().getTime();
            }
        }
        if ( !( preTimeA == aftTimeA && preTimeM < aftTimeM ) ) {
            Assert.fail( "failed to check lastAccessTime, " + "preTimeA="
                    + preTimeA + ", aftTimeA=" + aftTimeA + ", " + "preTimeM="
                    + preTimeM + ", aftTimeM=" + aftTimeM );
        }
    }

    private void readFile( ScmWorkspace ws ) throws Exception {
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            // read scmfile
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            fos = new FileOutputStream( new File( downloadPath ) );
            sis = ScmFactory.File.createInputStream( file );
            sis.read( fos );

            // check content
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
        }
    }
}
