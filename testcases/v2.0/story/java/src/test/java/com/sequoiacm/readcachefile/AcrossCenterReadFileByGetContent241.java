package com.sequoiacm.readcachefile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
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

/**
 * @Testcase:SCM241 A中心写文件，B中心调用getContent一次性读取文件 1、在分中心A写入文件；
 *                  2、分中心B调用getContent输出流方式一次性读取文件，即文件大小<=1M;
 * @author fanyu init in 2017.05.10; huangxiaoni modify in 2017.6.6;
 */

public class AcrossCenterReadFileByGetContent241 extends TestScmBase {
    private final int branSitesNum = 2;
    private boolean runSuccess = false;
    private SiteWrapper rootSite = null;
    private List< SiteWrapper > branSites = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;

    private String fileName = "readCacheFile239";
    private ScmId fileId = null;
    private int fileSize = 1024 * 1024 - 1;
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

            session = TestScmTools.createSession( branSites.get( 0 ) );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "fourSite", "net" })
    public void netTest() throws Exception {
        SiteWrapper[] expSites = new SiteWrapper[] { branSites.get( 0 ),
                branSites.get( 1 ) };

        // writeFileFromA;
        fileId = ScmFileUtils.create( ws, fileName + UUID.randomUUID(),
                filePath );
        this.readFileFromB( expSites );
        runSuccess = true;
    }

    @Test(groups = { "fourSite", "star" })
    public void starTest() throws Exception {
        SiteWrapper[] expSites = new SiteWrapper[] { branSites.get( 0 ),
                branSites.get( 1 ), rootSite };

        // writeFileFromA;
        fileId = ScmFileUtils.create( ws, fileName + UUID.randomUUID(),
                filePath );
        this.readFileFromB( expSites );
        runSuccess = true;
    }

    @AfterClass(alwaysRun = true)
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

    private void readFileFromB( SiteWrapper[] expSites ) throws Exception {
        ScmSession session = null;
        OutputStream fos = null;
        ScmInputStream sis = null;
        try {
            // login
            session = TestScmTools.createSession( branSites.get( 1 ) );
            ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsp.getName(),
                    session );

            // read content
            ScmFile scmfile = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile.initDownloadPath(
                    localPath, TestTools.getMethodName(),
                    Thread.currentThread().getId() );
            fos = new FileOutputStream( new File( downloadPath ) );
            scmfile.getContent( fos );

            // check results
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
            ScmFileUtils.checkMetaAndData( wsp, fileId, expSites, localPath,
                    filePath );
        } finally {
            if ( fos != null )
                fos.close();
            if ( sis != null )
                sis.close();
            if ( session != null )
                session.close();
        }
    }
}
