package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @descreption SCM-4282 :: 创建SCM文件，文件名包含特殊字符
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4282 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private String wsNameA = "ws_4282_A";
    private String wsNameB = "ws_4282_B";
    private ScmWorkspace ws_test_A = null;
    private ScmWorkspace ws_test_B = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4282";
    private String key = "object4282";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ScmWorkspaceUtil.deleteWs( wsNameA, session );
        ScmWorkspaceUtil.deleteWs( wsNameB, session );
        int siteNum = ScmInfo.getSiteNum();

        ws_test_A = ScmWorkspaceUtil.createWS( session, wsNameA, siteNum );
        ScmWorkspaceUtil.wsSetPriority( session, wsNameA );

        ws_test_B = ScmWorkspaceUtil.createS3WS( session, wsNameB );
        ScmWorkspaceUtil.wsSetPriority( session, wsNameB );
    }

    @Test
    public void test() throws ScmException, IOException {
        // "\%;:*?"<>|" will success if directory is enable
        ScmFile fileA = ScmFactory.File.createInstance( ws_test_A );
        fileA.setFileName( key + "\\%;:*?\"<>|" );
        fileA.setContent( filePath );
        fileA.save();

        // '/' failed if directory is enable
        try {
            ScmFile fileB = ScmFactory.File.createInstance( ws_test_A );
            fileB.setFileName( key + "/" );
            fileB.setContent( filePath );
            fileB.save();
            Assert.fail("'/' is invalid in file name if directory is enable");
        } catch ( ScmException e ) {
            System.out.println( "create file:" + bucketName + ", error:"
                    + e.getError().getErrorDescription() );
            Assert.assertEquals( "Invalid argument",
                    e.getError().getErrorDescription() );
        }

        // "/" success if directory is not enable
        ScmFile fileC = ScmFactory.File.createInstance( ws_test_B );
        fileC.setFileName( key + "/" );
        fileC.setContent( filePath );
        fileC.save();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                ScmWorkspaceUtil.deleteWs( wsNameA, session );
                ScmWorkspaceUtil.deleteWs( wsNameB, session );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
            if ( session != null ) {
                session.close();
            }
        }
    }
}
