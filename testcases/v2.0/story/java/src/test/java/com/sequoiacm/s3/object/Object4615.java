package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
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
 * @descreption SCM-4615 :: 使用SCM API创建桶，对象，指定桶名和对象名中包含中文字符
 * @author Zhaoyujing
 * @Date 2020/6/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4615 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "桶名4615";
    private String key = "aa/bb/对象4463";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private int objectNums = 300;
    private int bucket_number = 20;

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
        S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws ScmException, IOException {
        // scm create buckets
        List< String > localBucketNames = new ArrayList<>();
        for ( int i = 0; i < bucket_number; i++ ) {
            String bucketNameN = bucketName + "-" + i;
            ScmFactory.Bucket.createBucket( ws, bucketNameN );
            localBucketNames.add( bucketNameN );
        }

        List< String > scmBucketList = scmGetBuckets( bucketName );
        // TODO: SEQUOIACM-935 修改后才能放开
        // Assert.assertEqualsNoOrder( localBucketNames.toArray(),
        // scmBucketList.toArray() );

        List< String > s3BucketList = s3GetBuckets( bucketName );
        Assert.assertEqualsNoOrder( localBucketNames.toArray(),
                s3BucketList.toArray() );

        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        List< String > keyList = createScmFiles( bucket );

        List< String > scmKeyList = scmGetFiles();
        // TODO: SEQUOIACM-935 修改后才能放开
        // Assert.assertEqualsNoOrder( keyList.toArray(),
        // scmKeyList.toArray() );

        List< String > s3KeyList = s3GetFiles();
        Assert.assertEqualsNoOrder( keyList.toArray(), s3KeyList.toArray() );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketName );
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

    private List< String > createScmFiles( ScmBucket bucket )
            throws ScmException {
        List< String > keyList = new ArrayList<>();
        for ( int i = 0; i < objectNums; i++ ) {
            String keyName = key + "_" + i;
            keyList.add( keyName );
            ScmFile file = bucket.createFile( keyName );
            file.setContent( filePath );
            file.setAuthor( "作者4615" );
            file.save();
        }
        return keyList;
    }

    private List< String > s3GetBuckets( String bucketPrefix ) {
        List< String > bucketNames = new ArrayList<>();
        List< Bucket > buckets = s3Client.listBuckets();

        for ( int i = 0; i < buckets.size(); i++ ) {
            if ( buckets.get( i ).getName().startsWith( bucketPrefix ) ) {
                bucketNames.add( buckets.get( i ).getName() );
            }
        }

        return bucketNames;
    }

    private List< String > scmGetBuckets( String bucketPrefix )
            throws ScmException {
        List< String > bucketNames = new ArrayList<>();
        ScmCursor< ScmBucket > bucketCursor = ScmFactory.Bucket
                .listBucket( session, null, null );
        while ( bucketCursor.hasNext() ) {
            ScmBucket bucket = bucketCursor.getNext();
            if ( bucket.getName().startsWith( bucketPrefix ) ) {
                bucketNames.add( bucket.getName() );
            }
        }

        return bucketNames;
    }

    private List< String > scmGetFiles() throws ScmException {
        List< String > getKeyList = new ArrayList<>();

        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        ScmCursor< ScmFileBasicInfo > filesCursor = bucket.listFile( null, null,
                0, -1 );
        while ( filesCursor.hasNext() ) {
            ScmFileBasicInfo file = filesCursor.getNext();
            getKeyList.add( file.getFileName() );
        }

        return getKeyList;
    }

    private List< String > s3GetFiles() {
        List< String > getKeyList = new ArrayList<>();

        ObjectListing objectListing = s3Client.listObjects( bucketName );
        for ( int i = 0; i < objectListing.getObjectSummaries().size(); i++ ) {
            getKeyList.add(
                    objectListing.getObjectSummaries().get( i ).getKey() );
        }

        return getKeyList;
    }
}
