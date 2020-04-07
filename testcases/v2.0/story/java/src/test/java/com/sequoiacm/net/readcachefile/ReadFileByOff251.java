package com.sequoiacm.net.readcachefile;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmInputStream;
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
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmNetUtils;

/**
 * @Testcase: SCM-251:read文件，Byte长度为0，偏移+长度=0 1、分中心A写文件 2、分中心B调用read(byte[]b,
 *            int off, int len)读取文件,Byte长度为0，偏移+长度=0
 * @author huangxiaoni init
 * @date 2017.5.6
 */

public class ReadFileByOff251 extends TestScmBase {
    private boolean runSuccess = false;
    private SiteWrapper scmSite1 = null;
    private SiteWrapper scmSite2 = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "readCacheFile251";
    private ScmId fileId = null;
    private int fileSize = 0;
    private int off = 0;
    private int len = 1;
    private File localPath = null;
    private String filePath = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() throws IOException {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath =
                localPath + File.separator + "localFile_" + fileSize + ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath, fileSize );

            wsp = ScmInfo.getWs();
            List< SiteWrapper > siteList = ScmNetUtils.getRandomSites( wsp );
            scmSite1 = siteList.get( 0 );
            scmSite2 = siteList.get( 1 );

            session = TestScmTools.createSession( scmSite1 );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite" })
    private void test() throws Exception {
        try {
            // writeFileFromA
            fileId = ScmFileUtils.create( ws, fileName, filePath );
            this.readFileFromOtherSite( scmSite2 );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
        runSuccess = true;
    }

    @AfterClass()
    private void tearDown() {
        try {
            if ( runSuccess || forceClear ) {
                ScmFactory.File.getInstance( ws, fileId ).delete( true );
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }

        }
    }

    private void readFileFromOtherSite( SiteWrapper scmSite ) throws Exception {
        ScmSession session = null;
        ScmInputStream sis = null;
        try {
            // login
            session = TestScmTools.createSession( scmSite );
            ScmWorkspace ws = ScmFactory.Workspace
                    .getWorkspace( wsp.getName(), session );

            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            sis = ScmFactory.File.createInputStream( scmfile );
            byte[] buffer = new byte[ off + len ];
            int readSize = sis.read( buffer, off, len );
            Assert.assertEquals( readSize, -1 );

            // check results
            SiteWrapper[] expSites = { scmSite1, scmSite2 };
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                    filePath );
        } finally {
            if ( sis != null )
                sis.close();
            if ( session != null )
                session.close();
        }
    }

}