package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-4760 :: 指定最新版本号获取文件 ;SCM-4761 :: 指定历史版本号获取文件
 * @author wuyan
 * @Date 2022.07.09
 * @version 1.00
 */
public class ScmFile4760_4761 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4760";
    private String fileName = "scmfile4760";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 2;
    private int updateSize = 1024 * 3;
    private String filePath = null;
    private String updatePath = null;
    private File localPath = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private ScmSession session;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "localFile_" + updateSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, updateSize );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();

        fileId = S3Utils.createFile( scmBucket, fileName, filePath );
        S3Utils.createFile( scmBucket, fileName, updatePath );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // test4760:指定最新版本获取对象
        int currentVersion = 2;
        ScmFile file = scmBucket.getFile( fileName, currentVersion, 0 );
        checkFileAttributes( file, currentVersion, updateSize );
        S3Utils.checkFileContent( file, updatePath, localPath );

        // test4761:指定历史版本获取对象
        int historyVersion = 1;
        ScmFile file1 = scmBucket.getFile( fileName, historyVersion, 0 );
        checkFileAttributes( file1, historyVersion, fileSize );
        S3Utils.checkFileContent( file1, filePath, localPath );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( session, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFileAttributes( ScmFile file, int fileVersion,
            long fileSize ) {
        Assert.assertEquals( file.getWorkspaceName(), s3WorkSpaces );
        Assert.assertEquals( file.getFileId(), fileId );
        Assert.assertEquals( file.getFileName(), fileName );
        Assert.assertEquals( file.getTitle(), fileName );
        Assert.assertEquals( file.getSize(), fileSize );
        Assert.assertEquals( file.getMinorVersion(), 0 );
        Assert.assertEquals( file.getMajorVersion(), fileVersion );
        Assert.assertFalse( file.isNullVersion() );
    }
}
