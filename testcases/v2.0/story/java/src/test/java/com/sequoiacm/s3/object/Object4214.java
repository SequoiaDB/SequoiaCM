package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4214 :: SCM API创建S3文件，S3接口列取文件
 * @author Zhaoyujing
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4214 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4214";
    private String key = "aa/bb/object4214";
    private String prefix = "aa/";
    private String delimiter = "/";
    private String commonPrefix = "aa/bb/";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;
    private int objectNums = 30;

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
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws ScmException, IOException {
        // scm create bucket
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );

        // scm create file ,file name : prefix , delimiter
        List< String > keyList = createScmFiles( bucket );

        // s3 list objects
        ObjectListing objectListing = s3Client.listObjects( bucketName );
        // check objects
        checkObjectList( objectListing, keyList );

        // check common prefix
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName( bucketName ).withPrefix( prefix )
                .withDelimiter( delimiter );
        ObjectListing commonPrefixListing = s3Client.listObjects( request );
        Assert.assertEquals( commonPrefixListing.getCommonPrefixes().size(),
                1 );
        Assert.assertEquals( commonPrefixListing.getCommonPrefixes().get( 0 ),
                commonPrefix );

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

    private List< String > createScmFiles( ScmBucket bucket )
            throws ScmException {
        List< String > keyList = new ArrayList<>();
        for ( int i = 0; i < objectNums; i++ ) {
            String keyName = key + "_" + i;
            keyList.add( keyName );
            ScmFile file = bucket.createFile( keyName );

            file.setContent( filePath );
            file.setFileName( keyName );
            file.setAuthor( "author4214" );
            file.save();
        }
        return keyList;
    }

    private void checkObjectList( ObjectListing objectListing,
            List< String > localKeyList ) {
        List< String > getKeyList = new ArrayList<>();
        for ( int i = 0; i < objectListing.getObjectSummaries().size(); i++ ) {
            getKeyList.add(
                    objectListing.getObjectSummaries().get( i ).getKey() );
        }

        Assert.assertEqualsNoOrder( getKeyList.toArray(),
                localKeyList.toArray() );
    }
}
