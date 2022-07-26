package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.module.ScmBucketAttachFailure;
import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4269 :: 批量关联文件时，部分文件不存在
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4269 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4269";
    private String key = "aa/bb/object4269";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private int objectNums = 300;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws ScmException, IOException {
        // scm create bucket
        ScmFactory.Bucket.createBucket( ws, bucketName );
        S3Utils.updateBucketVersionConfig( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );

        // scm create files ,file name
        List< ScmId > keyList = new ArrayList<>();
        for ( int i = 0; i < objectNums; i++ ) {
            keyList.add( createScmFile( key + "_" + i ) );
        }

        // delete some files
        List< String > deletedList = new ArrayList<>();
        for ( int i = 0; i < objectNums / 10; i++ ) {
            ScmId deleteId = keyList.get( i );
            ScmFactory.File.deleteInstance( ws, deleteId, true );
            deletedList.add( deleteId.toString() );
        }

        // attach all files , include deleted files
        List< ScmBucketAttachFailure > failures = ScmFactory.Bucket.attachFile(
                session, bucketName, keyList,
                ScmBucketAttachKeyType.FILE_NAME );
        List< String > failureList = new ArrayList<>();
        for ( ScmBucketAttachFailure failure : failures ) {
            failureList.add( failure.getFileId() );
            Assert.assertEquals(failure.getError().getErrorDescription(), "File not found");
        }

        // check deletedList and failureList
        Assert.assertEqualsNoOrder( deletedList.toArray(),
                failureList.toArray() );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
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

    private ScmId createScmFile( String keyName ) throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( keyName );
        file.save( new ScmUploadConf( true, true ) );
        return file.getFileId();
    }
}
