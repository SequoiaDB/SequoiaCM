package com.sequoiacm.net.readcachefile;

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
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;

/**
 * @Testcase: SCM-249:read文件，偏移读取文件末尾部分 1、分中心A写文件 2、分中心B调用read(byte[]b, int off,
 *            int len)读取文件
 * @author huangxiaoni init
 * @date 2017.5.6
 * @modified By wuyan
 * @modified Date 2018.7.23
 */

public class ReadFileByOff249 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper scmSite1 = null;
    private SiteWrapper scmSite2 = null;
    private WsWrapper wsp = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;

    private String fileName = "readCacheFile249";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 3;
    private int seekSize = 1024 * 1024;
    private int off = 1024 * 1024 - 1;
    private int len = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass()
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        wsp = ScmInfo.getWs();
        List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( wsp );
        scmSite1 = siteList.get( 0 );
        scmSite2 = siteList.get( 1 );

        // clean file
        BSONObject cond = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( fileName ).get();
        ScmFileUtils.cleanFile( wsp, cond );

        sessionA = TestScmTools.createSession( scmSite1 );
        wsA = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionA );
        sessionB = TestScmTools.createSession( scmSite2 );
        wsB = ScmFactory.Workspace.getWorkspace( wsp.getName(), sessionB );
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        fileId = ScmFileUtils.create( wsA, fileName, filePath );
        this.readFileFromB( wsB );
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

    private void readFileFromB( ScmWorkspace ws ) throws Exception {
        OutputStream fos = null;
        ScmInputStream in = null;
        try {
            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );

            in = ScmFactory.File.createInputStream( InputStreamType.SEEKABLE,
                    scmfile );
            in.seek( SeekType.SCM_FILE_SEEK_SET, seekSize );
            fos = new FileOutputStream( new File( downloadPath ) );
            byte[] buffer = new byte[ off + len ];
            int curOff = 0;
            int curExpReadLen = 0;
            int curActReadLen = 0;
            int readSize = 0;
            while ( readSize < len ) {
                curOff = off + readSize;
                curExpReadLen = len - readSize;
                curActReadLen = in.read( buffer, curOff, curExpReadLen );
                if ( curActReadLen <= 0 ) {
                    break;
                }
                fos.write( buffer, off + readSize, curActReadLen );
                readSize += curActReadLen;
            }
            fos.flush();

            // check results
            String tmpPath = TestTools.LocalFile.initDownloadPath( localPath,
                    TestTools.getMethodName(), Thread.currentThread().getId() );
            TestTools.LocalFile.readFile( filePath, seekSize, len, tmpPath );
            Assert.assertEquals( TestTools.getMD5( tmpPath ),
                    TestTools.getMD5( downloadPath ) );

            SiteWrapper[] expSites = { scmSite1, scmSite2 };
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