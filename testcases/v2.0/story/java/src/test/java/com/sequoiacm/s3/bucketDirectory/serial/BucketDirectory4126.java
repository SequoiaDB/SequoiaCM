package com.sequoiacm.s3.bucketDirectory.serial;

import java.io.File;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;

/**
 * @description SCM-4126:并发多个线程创建工作区，指定不同的BucketName和objectSharding参数
 * @author ZhangYanan
 * @createDate 2022.2.18
 * @updateUser ZhangYanan
 * @updateDate 2022.2.18
 * @updateRemark
 * @version v1.0
 */
public class BucketDirectory4126 extends TestScmBase {
    private boolean runSuccess = false;
    private String wsName1 = "ws4126_1";
    private String wsName2 = "ws4126_2";
    private String wsName3 = "ws4126_3";
    private String bucketName1 = "bucketname14126";
    private String bucketName2 = "bucketname24126";
    private String bucketName3 = "bucketname34126";
    private ScmShardingType objectIDShardingType1 = ScmShardingType.YEAR;
    private ScmShardingType objectIDShardingType2 = ScmShardingType.MONTH;
    private ScmShardingType objectIDShardingType3 = ScmShardingType.NONE;
    private int threadSuccessNum = 0;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private AmazonS3 S3Client = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private String fileName = "file_4126";

    @BeforeClass
    private void setUp() throws Exception {
        site = ScmInfo.getSiteByType( ScmType.DatasourceType.CEPH_S3 );
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.createFile( filePath, fileSize );
        session = ScmSessionUtils.createSession( site );
        S3Client = CephS3Utils.createConnect( site );
        S3Client.createBucket( bucketName1 );
        S3Client.createBucket( bucketName2 );
        S3Client.createBucket( bucketName3 );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {

        ThreadExecutor es = new ThreadExecutor();
        es.addWorker( new ThreadCreateWS( wsName1, bucketName1,
                objectIDShardingType1 ) );
        es.addWorker( new ThreadCreateWS( wsName2, bucketName2,
                objectIDShardingType2 ) );
        es.addWorker( new ThreadCreateWS( wsName3, bucketName3,
                objectIDShardingType3 ) );
        es.run();

        Assert.assertEquals( threadSuccessNum, 3 );

        checkBucketNameAndObjectId( wsName1, bucketName1,
                objectIDShardingType1 );
        checkBucketNameAndObjectId( wsName2, bucketName2,
                objectIDShardingType2 );
        checkBucketNameAndObjectId( wsName3, bucketName3,
                objectIDShardingType3 );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess || forceClear ) {
            CephS3Utils.deleteBucket( S3Client, bucketName1 );
            CephS3Utils.deleteBucket( S3Client, bucketName2 );
            CephS3Utils.deleteBucket( S3Client, bucketName3 );
            ScmFactory.Workspace.deleteWorkspace( session, wsName1 );
            ScmFactory.Workspace.deleteWorkspace( session, wsName2 );
            ScmFactory.Workspace.deleteWorkspace( session, wsName3 );
            TestTools.LocalFile.removeFile( localPath );
        }
        if ( session != null ) {
            session.close();
        }
        if ( S3Client != null ) {
            S3Client.shutdown();
        }
    }

    private class ThreadCreateWS extends ResultStore {
        String bucketName;
        ScmShardingType objectIDShardingType;
        String wsName;

        public ThreadCreateWS( String wsName, String bucketName,
                ScmShardingType objectIDShardingType ) {
            this.wsName = wsName;
            this.bucketName = bucketName;
            this.objectIDShardingType = objectIDShardingType;
        }

        @ExecuteOrder(step = 1)
        private void exec() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( site );
                ScmCephS3DataLocation cephS3DataLocation = new ScmCephS3DataLocation(
                        site.getSiteName(), bucketName, objectIDShardingType );
                CephS3Utils.createWS( session, wsName, cephS3DataLocation );
                CephS3Utils.wsSetPriority( session, wsName,
                        ScmPrivilegeType.ALL );
                threadSuccessNum++;
            } finally {
                session.close();
            }
        }
    }

    public void checkBucketNameAndObjectId( String wsName, String expBucketName,
            ScmShardingType objectIDShardingType ) throws ScmException {
        String expObjectID;

        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmId fileId = ScmFileUtils.create( ws, fileName, filePath );

        expObjectID = CephS3Utils.getObjectID( wsName, objectIDShardingType,
                fileId );
        boolean doesObjectAndBucketExist = S3Client
                .doesObjectExist( expBucketName, expObjectID );

        Assert.assertTrue( doesObjectAndBucketExist );

        ScmFactory.File.deleteInstance( ws, fileId, true );
    }
}