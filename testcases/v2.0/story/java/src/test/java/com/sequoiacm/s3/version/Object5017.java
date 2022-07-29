package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Description SCM-5017:S3接口当前版本和历史版本都存null版本文件，SCM API获取/更新/删除文件
 * @author wuyan
 * @Date 2022.07.23
 * @version 1.00
 */
public class Object5017 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket5017";
    private String keyName = "object5017";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 3;
    private int updateSize = 1024;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private ScmSession session;
    private SiteWrapper site = null;
    private ScmBucket scmBucket = null;
    private ScmId fileId = null;
    private ScmWorkspace ws = null;
    private long bucketId;

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

        s3Client = S3Utils.buildS3Client();
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, keyName, new File( filePath ) );
        scmBucket = ScmFactory.Bucket.getBucket( session, bucketName );
        scmBucket.enableVersionControl();
        s3Client.putObject( bucketName, keyName, new File( updatePath ) );
        scmBucket.suspendVersionControl();
        s3Client.putObject( bucketName, keyName, new File( updatePath ) );
    }

    @Test
    public void testCreateObject() throws Exception {
        // scm接口获取文件元数据和内容
        int currentVersion = -2;
        bucketId = scmBucket.getId();
        ScmFile file = scmBucket.getFile( keyName, currentVersion, 0 );
        fileId = file.getFileId();
        int serialVersion = 3;
        checkFileAttributes( file, currentVersion, updateSize, serialVersion );
        S3Utils.checkFileContent( file, updatePath, localPath );

        // 检查更新后属性，当前版本为更新文件，原文件为历史版本
        int newSerialVersion = 4;
        ScmFile curfile = scmBucket.getFile( keyName );
        curfile.updateContent( filePath );
        checkFileAttributes( curfile, currentVersion, fileSize,
                newSerialVersion );
        S3Utils.checkFileContent( curfile, filePath, localPath );

        // 指定删除null版本（-2）
        scmBucket.deleteFileVersion( keyName, currentVersion, 0 );
        // 获取当前版本为v2版本
        int version = 2;
        ScmFile newCurFile = scmBucket.getFile( keyName );
        checkFileAttributes( newCurFile, version, updateSize, version );
        S3Utils.checkFileContent( newCurFile, updatePath, localPath );
        checklistInstance( version );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( s3Client, bucketName );
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

    private void checkFileAttributes( ScmFile file, int fileVersion,
            long fileSize, int majorSerialVersion ) {
        Assert.assertEquals( file.getWorkspaceName(), s3WorkSpaces );
        Assert.assertEquals( file.getFileId(), fileId );
        Assert.assertEquals( file.getFileName(), keyName );
        Assert.assertEquals( file.getBucketId().longValue(), bucketId );

        Assert.assertEquals( file.getSize(), fileSize );
        Assert.assertEquals( file.getMinorVersion(), 0 );
        Assert.assertEquals( file.getMajorVersion(), fileVersion );
        Assert.assertEquals( file.getVersionSerial().getMajorSerial(),
                majorSerialVersion );
        if ( file.getMajorVersion() == -2 ) {
            Assert.assertTrue( file.isNullVersion() );
        } else {
            Assert.assertFalse( file.isNullVersion() );
        }
    }

    private void checklistInstance( int version ) throws ScmException {
        BSONObject condition = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_ID ).is( fileId.get() )
                .get();
        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance( ws,
                ScmType.ScopeType.SCOPE_ALL, condition );
        int size = 0;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo file = cursor.getNext();
            Assert.assertEquals( file.getMajorVersion(), version );
            Assert.assertEquals( file.getVersionSerial().getMajorSerial(),
                    version );
            Assert.assertFalse( file.isNullVersion(),
                    "---key version =" + file.getMajorVersion() );
            size++;
        }
        cursor.close();
        // exist 1 current version file
        int expFileNum = 1;
        Assert.assertEquals( size, expFileNum );
    }
}
