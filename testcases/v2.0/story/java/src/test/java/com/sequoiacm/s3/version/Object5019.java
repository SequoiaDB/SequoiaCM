package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description SCM-5019:SCM API创建多版本文件（存在null版本），S3接口列取版本文件
 * @author wuyan
 * @Date 2022.07.26
 * @version 1.00
 */
public class Object5019 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket5019";
    private String keyName1 = "object5019a";
    private String keyName2 = "object5019b";
    private String keyName3 = "object5019c";
    private String keyName4 = "object5019d";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024;
    private int updateSize = 1024 * 2;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private ScmSession session;
    private SiteWrapper site = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private List< String > expKeyAndVersions = new ArrayList<>();

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
        session = ScmSessionUtils.createSession( site );
        S3Utils.clearBucket( session, bucketName );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        // key1:v1、v2、v3（null）、v4
        // keyName2：v1（null）、v2
        // keyName3: v1（null）、v2（null）
        // keyName4：v1、v2、v3、v4（null）
        ScmFileUtils.createFile( scmBucket, keyName2, filePath );
        ScmFileUtils.createFile( scmBucket, keyName3, filePath );
        scmBucket.enableVersionControl();
        ScmFileUtils.createFile( scmBucket, keyName1, filePath );
        ScmFileUtils.createFile( scmBucket, keyName1, updatePath );
        ScmFileUtils.createFile( scmBucket, keyName2, updatePath );
        ScmFileUtils.createFile( scmBucket, keyName4, filePath );
        ScmFileUtils.createFile( scmBucket, keyName4, filePath );
        ScmFileUtils.createFile( scmBucket, keyName4, updatePath );
        scmBucket.suspendVersionControl();
        ScmFileUtils.createFile( scmBucket, keyName1, filePath );
        ScmFileUtils.createFile( scmBucket, keyName3, updatePath );
        ScmFileUtils.createFile( scmBucket, keyName4, updatePath );
        scmBucket.enableVersionControl();
        ScmFileUtils.createFile( scmBucket, keyName1, updatePath );
        expKeyAndVersions = generateExpectResult();
        s3Client = S3Utils.buildS3Client();
    }

    @Test
    public void testCreateObject() throws Exception {
        VersionListing versionList = s3Client.listVersions(
                new ListVersionsRequest().withBucketName( bucketName ) );
        List< S3VersionSummary > verList = versionList.getVersionSummaries();
        checkListObjectsResult( verList, expKeyAndVersions );
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

    private void checkListObjectsResult( List< S3VersionSummary > versionLists,
            List< String > expKeyAndVersions ) {
        List< String > actKeyAndVersions = new ArrayList<>();

        for ( int i = 0; i < versionLists.size(); i++ ) {
            String key = versionLists.get( i ).getKey();
            String version = versionLists.get( i ).getVersionId();
            actKeyAndVersions.add( key + ":" + version );
        }
        Assert.assertEquals( actKeyAndVersions, expKeyAndVersions, "---actkeys="
                + actKeyAndVersions + "/n---expKeys=" + expKeyAndVersions );
    }

    private List< String > generateExpectResult() {
        List< String > keyAndVersions = new ArrayList<>();
        keyAndVersions.add( keyName1 + ":4.0" );
        keyAndVersions.add( keyName1 + ":null" );
        keyAndVersions.add( keyName1 + ":2.0" );
        keyAndVersions.add( keyName1 + ":1.0" );
        keyAndVersions.add( keyName2 + ":2.0" );
        keyAndVersions.add( keyName2 + ":null" );
        keyAndVersions.add( keyName3 + ":null" );
        keyAndVersions.add( keyName4 + ":null" );
        keyAndVersions.add( keyName4 + ":3.0" );
        keyAndVersions.add( keyName4 + ":2.0" );
        keyAndVersions.add( keyName4 + ":1.0" );
        return keyAndVersions;
    }
}
