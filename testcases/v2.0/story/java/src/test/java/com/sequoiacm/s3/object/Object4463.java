package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
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
 * @descreption SCM-4463 :: 批量关联文件时，部分文件已关联到不同桶中
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4463 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketNameA = "bucket4463-a";
    private String bucketNameB = "bucket4463-b";
    private String key = "aa/bb/object4463";
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
        S3Utils.clearBucket( s3Client, bucketNameA );
        S3Utils.clearBucket( s3Client, bucketNameB );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws ScmException, IOException {
        // scm create buckets
        ScmFactory.Bucket.createBucket( ws, bucketNameA );
        ScmFactory.Bucket.createBucket( ws, bucketNameB );
        S3Utils.updateBucketVersionConfig( s3Client, bucketNameA,
                BucketVersioningConfiguration.ENABLED );
        S3Utils.updateBucketVersionConfig( s3Client, bucketNameB,
                BucketVersioningConfiguration.ENABLED );

        // scm create files ,file name
        List< ScmId > keyList = new ArrayList<>();
        for ( int i = 0; i < objectNums; i++ ) {
            keyList.add( createScmFile( key + "_" + i ) );
        }

        // attach some files to bucketA
        List< String > attachedList = new ArrayList<>();
        for ( int i = 0; i < objectNums / 10; i++ ) {
            ScmId attachId = keyList.get( i );
            ScmFactory.Bucket.attachFile( session, bucketNameA, attachId );
            attachedList.add( attachId.toString() );
        }

        // attach all files to bucketB, include attached files
        List< ScmBucketAttachFailure > failures = ScmFactory.Bucket.attachFile(
                session, bucketNameB, keyList, ScmBucketAttachKeyType.FILE_ID );

        // check deletedList and failureList
        List< String > failureList = new ArrayList<>();
        for ( ScmBucketAttachFailure failure : failures ) {
            failureList.add( failure.getFileId() );
            Assert.assertEquals( "FILE_IN_ANOTHER_BUCKET",
                    failure.getError().getErrorType() );
            Assert.assertTrue( failure.getExternalInfo().get( "bucket_name" )
                    .equals( bucketNameA ) );
        }
        Assert.assertEqualsNoOrder( attachedList.toArray(),
                failureList.toArray() );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketNameA );
                S3Utils.clearBucket( s3Client, bucketNameB );
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
