package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
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
import java.util.Arrays;
import java.util.List;

/**
 * @descreption SCM-4281 :: 使用SCM API创建S3文件，文件名包含特殊字符
 * @author Zhaoyujing
 * @Date 2020/5/26
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4281 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4281";
    private String key = "object4281";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private List< String > objectNameList = new ArrayList< String >();
    private List< String > specialObjectChars = Arrays.asList( "/", "\\", "%", ";",
            ":", "*", "?", "\"", "<", ">", "|" );

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
        for (String specialChar:specialObjectChars) {
            objectNameList.add( key + specialChar );
            objectNameList.add( key + specialChar + key );
            objectNameList.add( specialChar + key );
        }

        // create object success
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        for ( int i = 0; i < objectNameList.size(); i++ ) {
            try {
                ScmFile file = bucket.createFile( objectNameList.get( i ) );
                file.setContent( filePath );
                file.setFileName( objectNameList.get( i ) );
                file.save( new ScmUploadConf( true, true ) );
            } catch ( ScmException e ) {
                Assert.fail( "Create file：" + objectNameList.get( i )
                        + " should success" );
            }
        }

        // list objects success
        List< String > getObjectList = new ArrayList<>();
        ObjectListing objectListing = s3Client.listObjects( bucketName );
        List< S3ObjectSummary > objectSummaries = objectListing
                .getObjectSummaries();
        for ( S3ObjectSummary objectSummary : objectSummaries ) {
            getObjectList.add( objectSummary.getKey() );
        }
        Assert.assertEqualsNoOrder( objectNameList.toArray(),
                getObjectList.toArray() );

        // get objects success
        String fileMD5 = TestTools.getMD5( filePath );
        for ( int i = 0; i < objectNameList.size(); i++ ) {
            S3Object object = s3Client.getObject( bucketName,
                    objectNameList.get( i ) );
            Assert.assertEquals( object.getObjectMetadata().getETag(),
                    fileMD5 );
        }

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
}
