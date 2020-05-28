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
import org.testng.annotations.DataProvider;
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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @Testcase: SCM-243:seek文件，偏移<文件长度 1、分中心A写文件 2、分中心B seek文件，size<文件长度
 *            3、调用read(OutPutStream out)读取文件
 * @author huangxiaoni init
 * @date 2017.5.5
 * @modified By wuyan
 * @modified Date 2018.7.23
 */

public class SeekReadFile243 extends TestScmBase {
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;
    private String fileName = "readCacheFile243";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;

    @DataProvider(name = "seekSizeProvider")
    public Object[][] generateSeekSize() {
        return new Object[][] {
                // the parameter : seekSize
                // seekSize < fileSize
                new Object[] { 1024 * 2 },
                // seekSize = fileSize - 1
                new Object[] { 1024 * 1024 - 1 },
                // seekSize = 1
                new Object[] { 1 }, };
    }

    @BeforeClass()
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

        sessionA = TestScmTools.createSession( branSites.get( 0 ) );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = TestScmTools.createSession( branSites.get( 1 ) );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
        fileId = ScmFileUtils.create( wsA, fileName, filePath );
    }

    @Test(groups = { "fourSite" }, dataProvider = "seekSizeProvider")
    private void test( int seekSize ) throws Exception {
        this.readFileFromB( wsB, seekSize );
        runSuccess = true;
    }

    @AfterClass()
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

    private void readFileFromB( ScmWorkspace ws, int seekSize )
            throws Exception {

        OutputStream fos = null;
        ScmInputStream in = null;
        try {
            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            fos = new FileOutputStream( new File( downloadPath ) );

            in = ScmFactory.File.createInputStream( InputStreamType.SEEKABLE,
                    scmfile );
            in.seek( SeekType.SCM_FILE_SEEK_SET, seekSize );
            in.read( fos );

            // check results
            String tmpPath = TestTools.LocalFile.initDownloadPath( localPath,
                    TestTools.getMethodName(), Thread.currentThread().getId() );
            TestTools.LocalFile.readFile( filePath, seekSize, tmpPath );
            Assert.assertEquals( TestTools.getMD5( tmpPath ),
                    TestTools.getMD5( downloadPath ) );

            SiteWrapper[] expSites = { rootSite, branSites.get( 0 ),
                    branSites.get( 1 ) };
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                    filePath );
        } finally {
            if ( fos != null )
                fos.close();
            if ( in != null )
                in.close();
        }
    }

}