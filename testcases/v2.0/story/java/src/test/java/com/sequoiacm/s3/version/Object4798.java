package com.sequoiacm.s3.version;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.client.core.ScmBucket;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.SiteWrapper;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @Description SCM-4798 :: SCM创建多版本文件，S3指定版本删除deleteMarker标记对象
 * @author wuyan
 * @Date 2022.07.15
 * @version 1.00
 */
public class Object4798 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4798a";
    private String keyName = "对象object4798a";
    private AmazonS3 s3Client = null;
    private int fileSize = 1024 * 1024;
    private byte[] filedata = new byte[ fileSize ];
    private ScmSession session;
    private SiteWrapper site = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        S3Utils.clearBucket( session, bucketName );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        new Random().nextBytes( filedata );
        S3Utils.createFile( scmBucket, keyName, filedata );
        scmBucket.deleteFile( keyName, false );
        scmBucket.deleteFile( keyName, false );
        scmBucket.deleteFile( keyName, false );
        s3Client = S3Utils.buildS3Client();
    }

    @Test(groups = { GroupTags.base })
    public void testCreateObject() throws Exception {
        String versionV4 = "4.0";
        String versionV3 = "3.0";
        String versionV2 = "2.0";
        String versionV1 = "1.0";

        // 场景a：指定当前版本v4删除标记
        s3Client.deleteVersion( bucketName, keyName, versionV4 );
        ArrayList< String > versions = new ArrayList<>();
        ListVersionsRequest request = new ListVersionsRequest()
                .withBucketName( bucketName );
        VersionListing versionList = s3Client.listVersions( request );
        List< S3VersionSummary > verList = versionList.getVersionSummaries();
        for ( int i = 0; i < verList.size(); i++ ) {
            S3VersionSummary versionInfo = verList.get( i );
            String version = versionInfo.getVersionId();
            versions.add( version );
            // v1版本存在对象数据，非删除标记版本
            if ( version.equals( versionV1 ) ) {
                Assert.assertFalse( versionInfo.isDeleteMarker() );
                Assert.assertEquals( versionInfo.getSize(), fileSize );
            } else {
                Assert.assertTrue( versionInfo.isDeleteMarker() );
            }

            if ( version.equals( versionV4 ) ) {
                Assert.fail( "---delete Marker should be deleted!" );
            }
        }
        int objectVersionNum = 3;
        Assert.assertEquals( versions.size(), objectVersionNum,
                "---object version are  " + versions );

        // 场景b：指定历史版本v2删除标记
        s3Client.deleteVersion( bucketName, keyName, versionV2 );
        ArrayList< String > versions1 = new ArrayList<>();
        ListVersionsRequest request1 = new ListVersionsRequest()
                .withBucketName( bucketName );
        VersionListing versionList1 = s3Client.listVersions( request1 );
        List< S3VersionSummary > verList1 = versionList1.getVersionSummaries();
        for ( int i = 0; i < verList1.size(); i++ ) {
            S3VersionSummary versionInfo1 = verList1.get( i );
            String version1 = versionInfo1.getVersionId();
            versions1.add( version1 );
            // v1版本存在对象数据，非删除标记版本
            if ( version1.equals( versionV1 ) ) {
                Assert.assertFalse( versionInfo1.isDeleteMarker() );
                Assert.assertEquals( versionInfo1.getSize(), fileSize );
            } else {
                Assert.assertTrue( versionInfo1.isDeleteMarker() );
            }

            if ( version1.equals( versionV2 ) ) {
                Assert.fail( "---delete Marker should be deleted!" );
            }
        }
        int objectVersionNum1 = 2;
        Assert.assertEquals( versions1.size(), objectVersionNum1,
                "---object version are  " + versions1 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                S3Utils.clearBucket( s3Client, bucketName );
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
