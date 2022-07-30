package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4723 :: 指定prefix为空串查询对象版本列表 
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4723 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4723";
    private String[] objectNames = { "dir1/4723", "dir1/dir2/4723",
            "dir1/dir3/4723", "4723" };
    private List< String > objectNameList = new ArrayList<>();
    private String prefix = "";
    private int versionNum = 10;
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 2;
    private File localPath = null;
    private String filePath = null;

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
        s3Client.createBucket( bucketName );
        S3Utils.setBucketVersioning( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
        for ( String objectName : objectNames ) {
            for ( int i = 0; i < versionNum; i++ ) {
                objectNameList.add( objectName );
                s3Client.putObject( bucketName, objectName,
                        new File( filePath ) );
            }
        }
    }

    @Test
    public void testListObjects() throws Exception {
        ListVersionsRequest request = new ListVersionsRequest()
                .withBucketName( bucketName );
        request.withPrefix( prefix );
        VersionListing vsList = s3Client.listVersions( request );
        MultiValueMap< String, String > expMap = new LinkedMultiValueMap<>();
        for ( int i = 0; i < objectNames.length; i++ ) {
            for ( int j = versionNum; j > 0; j-- ) {
                expMap.add( objectNames[ i ], j + ".0" );
            }
        }
        // check
        Assert.assertEquals( vsList.isTruncated(), false,
                "vsList.isTruncated() must be false" );
        S3Utils.checkListVSResults( vsList, new ArrayList< String >(),
                expMap );

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
            s3Client.shutdown();
        }
    }
}
