package com.sequoiacm.s3.version.concurrent;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * @Description: SCM-4872 :: 开启版本控制，并发列取文件版本列表
 * @author wuyan
 * @Date 2022.07.20
 * @version 1.00
 */
public class Object4872 extends TestScmBase {
    private boolean runSuccess = false;
    private String bucketName = "bucket4872";
    private String keyName = "key4872";
    private ScmId fileId = null;
    private List< ScmId > fileIds = new ArrayList<>();
    private int fileSize = 1024 * 1024 * 2;
    private int updateSize = 1024 * 1024 * 3;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private ScmBucket scmBucket = null;
    private int fileNums = 5;
    private List< String > expKeys = new ArrayList<>();

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
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, bucketName );
        scmBucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        scmBucket.enableVersionControl();
        s3Client = S3Utils.buildS3Client();
        for ( int i = 0; i < fileNums; i++ ) {
            String key = keyName + "_" + i;
            ScmId fileId = ScmFileUtils.createFile( scmBucket, key, filePath );
            ScmFileUtils.createFile( scmBucket, key, updatePath );
            fileIds.add( fileId );
            expKeys.add( key );
        }
    }

    @Test
    public void testCreateObject() throws Exception {
        ThreadExecutor es = new ThreadExecutor();
        ScmListFile scmListFile = new ScmListFile();
        S3ListVersions s3ListVersions = new S3ListVersions();
        es.addWorker( scmListFile );
        es.addWorker( s3ListVersions );
        es.run();

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

    private class ScmListFile {
        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            BSONObject condition = ScmQueryBuilder
                    .start().or( fileIdBSON( 0 ), fileIdBSON( 1 ),
                            fileIdBSON( 2 ), fileIdBSON( 3 ), fileIdBSON( 4 ) )
                    .get();
            ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File
                    .listInstance( ws, ScmType.ScopeType.SCOPE_ALL, condition );
            int curVersion = 2;
            int hisVersion = 1;
            HashSet< String > actFileNames = new HashSet<>();
            while ( cursor.hasNext() ) {
                ScmFileBasicInfo file = cursor.getNext();
                int version = file.getMajorVersion();
                String fileName = file.getFileName();
                if ( version == curVersion ) {
                    actFileNames.add( fileName );
                }
                if ( version == hisVersion ) {
                    actFileNames.add( fileName );
                }
            }
            cursor.close();

            Assert.assertEqualsNoOrder( actFileNames.toArray(),
                    expKeys.toArray(),
                    "act fileName are :" + actFileNames.toString() );
        }
    }

    private class S3ListVersions {
        String curVersion = "2.0";
        String hisVersion = "1.0";

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            HashSet< String > actKeyNames = new HashSet<>();
            VersionListing verList = s3Client.listVersions(
                    new ListVersionsRequest().withBucketName( bucketName ) );
            while ( true ) {
                Iterator< S3VersionSummary > versionIter = verList
                        .getVersionSummaries().iterator();

                while ( versionIter.hasNext() ) {
                    S3VersionSummary vs = versionIter.next();

                    Assert.assertEquals( vs.getBucketName(), bucketName,
                            "bucketName is wrong!" );
                    String keyName = vs.getKey();
                    String version = vs.getVersionId();
                    if ( version.equals( curVersion ) ) {
                        Assert.assertEquals( vs.getSize(), updateSize );
                        Assert.assertEquals( vs.getETag(),
                                TestTools.getMD5( updatePath ) );
                        actKeyNames.add( keyName );
                    } else {
                        Assert.assertEquals( version, hisVersion );
                        Assert.assertEquals( vs.getSize(), fileSize );
                        Assert.assertEquals( vs.getETag(),
                                TestTools.getMD5( filePath ) );
                        actKeyNames.add( keyName );
                    }
                }

                if ( verList.isTruncated() ) {
                    verList = s3Client.listNextBatchOfVersions( verList );
                } else {
                    break;
                }
            }

            Assert.assertEqualsNoOrder( actKeyNames.toArray(),
                    expKeys.toArray(),
                    "act fileName are :" + actKeyNames.toString() );
        }

    }

    private BSONObject fileIdBSON( int i ) throws ScmException {
        return ScmQueryBuilder.start( ScmAttributeName.File.FILE_ID )
                .is( fileIds.get( i ).get() ).get();
    }
}
