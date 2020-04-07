package com.sequoiacm.multids;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
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

/**
 * @Testcase: SCM-1079:ceph数据源，上传<=5M的文件
 * @author huangxiaoni init
 * @date 2018.1.22
 */

public class Ceph_writeFile_lte5M_1079 extends TestScmBase {
    private static SiteWrapper site = null;
    private static WsWrapper wsp = null;
    private static ScmSession session = null;
    private final int fileSize_3m = 3 * 1024 * 1024;
    private final int fileSize_5m = 5 * 1024 * 1024;
    private boolean runSuccess = false;
    private ScmWorkspace ws = null;
    private String fileName = "scm1079";
    private List< ScmId > fileIds = new ArrayList<>();
    private File localPath = null;
    private String filePath_3m = null;
    private String filePath_5m = null;

    @BeforeClass(alwaysRun = true)
    private void setUp() {
        localPath = new File( TestScmBase.dataDirectory + File.separator +
                TestTools.getClassName() );
        filePath_3m = localPath + File.separator + "localFile_" + fileSize_3m +
                ".txt";
        filePath_5m = localPath + File.separator + "localFile_" + fileSize_3m +
                ".txt";
        try {
            TestTools.LocalFile.removeFile( localPath );
            TestTools.LocalFile.createDir( localPath.toString() );
            TestTools.LocalFile.createFile( filePath_3m, fileSize_3m );
            TestTools.LocalFile.createFile( filePath_5m, fileSize_5m );

            site = ScmInfo.getSite();
            wsp = ScmInfo.getWs();
            session = TestScmTools.createSession( site );
            ws = ScmFactory.Workspace.getWorkspace( wsp.getName(), session );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }

    @Test(groups = { "oneSite", "twoSite", "fourSite" })
    private void test() {
        // 3m file
        ScmId fileId = this.writeScmFile( fileSize_3m, filePath_3m );
        this.readScmFile( fileId, filePath_3m );

        // 5m file
        fileId = this.writeScmFile( fileSize_5m, filePath_5m );
        this.readScmFile( fileId, filePath_5m );

        runSuccess = true;
    }

    private ScmId writeScmFile( int fileSize, String filePath ) {
        ScmId fileId = null;
        try {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setContent( filePath );
            file.setFileName( fileName + "_" + UUID.randomUUID() );
            file.setTitle( "sequoiacm" );
            fileId = file.save();
            fileIds.add( fileId );
        } catch ( ScmException e ) {
            Assert.fail( e.getMessage() );
        }
        return fileId;
    }

    private void readScmFile( ScmId fileId, String filePath ) {
        try {
            ScmFile file = ScmFactory.File.getInstance( ws, fileId );
            String downloadPath = TestTools.LocalFile
                    .initDownloadPath( localPath, TestTools.getMethodName(),
                            Thread.currentThread().getId() );
            file.getContent( downloadPath );
            Assert.assertEquals( TestTools.getMD5( filePath ),
                    TestTools.getMD5( downloadPath ) );
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        }
    }

    @AfterClass(alwaysRun = true)
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                for ( ScmId fileId : fileIds ) {
                    ScmFactory.File.deleteInstance( ws, fileId, true );
                }
                TestTools.LocalFile.removeFile( localPath );
            }
        } catch ( Exception e ) {
            Assert.fail( e.getMessage() );
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

}