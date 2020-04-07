package com.sequoiacm.net.readcachefile;

import java.io.File;
import java.io.IOException;
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
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.WsWrapper;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;

/**
 * @Testcase: SCM-250:read文件，偏移+长度>byte总长度 1、分中心A写文件 2、分中心B调用read(byte[]b, int
 *            off, int len)读取文件,偏移+长度>byte总长度
 * @author huangxiaoni init
 * @date 2017.5.6
 * @modify By wuyan
 * @modify Date: 2018.07.31
 * @version 1.10
 */

public class ReadFileByOff250 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper scmSite1 = null;
    private SiteWrapper scmSite2 = null;
    private WsWrapper wsp = null;
    private ScmSession sessionA = null;
    private ScmWorkspace wsA = null;
    private ScmSession sessionB = null;
    private ScmWorkspace wsB = null;

    private String fileName = "readCacheFile250";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 * 2;
    private int seekSize = 0;
    private int off = 1024 * 1024 - 1;
    private int len = 1024 * 1024 + 1;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass()
    private void setUp() throws IOException, ScmException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        wsp = ScmInfo.getWs();
        List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( wsp );
        scmSite1 = siteList.get( 0 );
        scmSite2 = siteList.get( 1 );

        //clean file
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
        fileId = ScmFileUtils.create( wsB, fileName, filePath );
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
        } catch ( Exception e ) {
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
        ScmInputStream in = null;
        try {
            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            in = ScmFactory.File
                    .createInputStream( InputStreamType.SEEKABLE, scmfile );
            in.seek( SeekType.SCM_FILE_SEEK_SET, seekSize );
            byte[] buffer = new byte[ off + len - 1 ];
            in.read( buffer, off, len );
            Assert.assertFalse( true,
                    "expect result is fail but actual is success." );
        } catch ( ScmException e ) {
            if ( e.getError() != ScmError.INVALID_ARGUMENT
                    || !e.getMessage().contains( "indexOutOfBound," +
                    "arraySize:2097151,off:1048575,len:1048577" ) ) {
                e.printStackTrace();
                Assert.fail( e.getMessage() );
            }
        } finally {
            if ( in != null )
                in.close();
        }
    }

}