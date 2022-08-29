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
 * @Description SCM-4762 :: 指定版本号为null-marker获取带null-marker标记的文件
 * @author wuyan
 * @Date 2022.07.11
 * @version 1.00
 */
public class ScmFile4762 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4762";
    private String fileName = "scmfile4762";
    private ScmId fileId = null;
    private SiteWrapper site = null;
    private int fileSize = 1024 * 5;
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
        fileId = S3Utils.createFile( scmBucket, fileName, filePath );

        // 桶启用版本控制后再次创建同名文件
        scmBucket.enableVersionControl();
        S3Utils.createFile( scmBucket, fileName, updatePath );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        ScmFile file = scmBucket.getNullVersionFile( fileName );
        int fileVersion = -2;
        checkFileAttributes( file, fileVersion, fileSize );
        S3Utils.checkFileContent( file, filePath, localPath );

        //获取当前版本文件
        ScmFile curFile = scmBucket.getFile(fileName);
        int curFileVersion = 2;
        checkFileAttributes( curFile, curFileVersion, updateSize );
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
        //-2为null版本
        if( fileVersion == -2 ){
            Assert.assertTrue( file.isNullVersion() );
            Assert.assertEquals(file.getVersionSerial().getMajorSerial(), 1);
        }else {
            Assert.assertFalse( file.isNullVersion() );
        }

    }
}
