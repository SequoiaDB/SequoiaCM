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

import com.sequoiacm.client.common.ScmType.InputStreamType;
import com.sequoiacm.client.core.ScmAttributeName;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
import com.sequoiacm.client.core.ScmQueryBuilder;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.CommonDefine.SeekType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-927:createInputStream指定type为UNSEEKABLE，并调用seek接口读取文件 *
 *
 * @author wuyan init
 * @date 2018.7.23
 */

public class SetUnseekable927 extends TestScmBase {
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;

    private String fileName = "readCacheFile927";
    private ScmId fileId = null;
    private int fileSize = 1024 * 3;
    private int seekSize = 1024 * 1;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        rootSite = ScmInfo.getRootSite();
        branSites = ScmInfo.getBranchSites( branSitesNum );
        wsp = ScmInfo.getWs();

        // clean file
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        sessionA = ScmSessionUtils.createSession( branSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = ScmSessionUtils.createSession( branSites.get( 1 ) );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );

    }

    @Test(groups = { "fourSite", "net" })
    public void nettest() throws Exception {
        SiteWrapper[] expSites = new SiteWrapper[] { branSites.get( 0 ),
                branSites.get( 1 ) };
        fileId = ScmFileUtils.create( wsA, fileName, filePath );
        readFileFromB( wsB, expSites );
        runSuccess = true;
    }

    @Test(groups = { "fourSite", "star" })
    public void startest() throws Exception {
        SiteWrapper[] expSites = new SiteWrapper[] { rootSite,
                branSites.get( 0 ), branSites.get( 1 ) };
        fileId = ScmFileUtils.create( wsA, fileName, filePath );
        readFileFromB( wsB, expSites );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.deleteInstance( wsA, fileId, true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( ScmException e ) {
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

    private void readFileFromB( ScmWorkspace ws, SiteWrapper[] expSites )
            throws Exception {
        // read content
        ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        OutputStream fos = new FileOutputStream( new File( downloadPath ) );

        ScmInputStream in = ScmFactory.File
                .createInputStream( InputStreamType.UNSEEKABLE, scmfile );
        try {
            in.seek( SeekType.SCM_FILE_SEEK_SET, seekSize );
            in.read( fos );
            Assert.fail( " seek must be fail!" );
        } catch ( ScmException e ) {
            if ( ScmError.OPERATION_UNSUPPORTED != e.getError() ) {
                Assert.fail( "expErrorCode:-107  actError:" + e.getError()
                        + e.getMessage() );
            }
        }

        // read all file
        OutputStream fileOutputStream = new FileOutputStream(
                new File( downloadPath ) );
        byte[] buffer = new byte[ fileSize ];
        while ( true ) {
            int readSize = in.read( buffer, 0, fileSize );
            if ( readSize == -1 ) {
                break;
            }
            fileOutputStream.write( buffer, 0, readSize );
        }
        fileOutputStream.close();

        // check read content
        String tmpPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        TestTools.LocalFile.readFile( filePath, 0, tmpPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( tmpPath ) );

        // check file sites
        ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                filePath );

        fos.close();
        in.close();
    }
}
