package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
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
    private String bucketNameA = "桶名4615";
    private String bucketNameB = "bucket4615a";
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
        S3Utils.clearBucket( s3Client, bucketNameB );
        S3Utils.deleteEmptyBucketsWithPrefix( s3Client, bucketNameB );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws ScmException, IOException {
        // scm create buckets
        try {
            ScmFactory.Bucket.createBucket( ws, bucketNameA );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError().getErrorType(),
                    "INVALID_ARGUMENT" );
        }

        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketNameB );
        List< String > keyList = createScmFiles( bucket );

        List< String > scmKeyList = scmGetFiles();
        Assert.assertEqualsNoOrder( keyList.toArray(), scmKeyList.toArray() );

        List< String > s3KeyList = s3GetFiles();
        Assert.assertEqualsNoOrder( keyList.toArray(), s3KeyList.toArray() );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess ) {
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

    private List< String > scmGetFiles() throws ScmException {
        List< String > getKeyList = new ArrayList<>();

        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketNameB );
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

        ObjectListing objectListing = s3Client.listObjects( bucketNameB );
        for ( int i = 0; i < objectListing.getObjectSummaries().size(); i++ ) {
            getKeyList.add(
                    objectListing.getObjectSummaries().get( i ).getKey() );
        }

        return getKeyList;
    }
}
