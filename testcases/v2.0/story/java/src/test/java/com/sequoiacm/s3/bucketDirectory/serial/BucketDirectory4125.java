package com.sequoiacm.s3.bucketDirectory.serial;

import java.io.File;

import com.sequoiacm.testcommon.listener.GroupTags;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmCephS3DataLocation;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @description SCM-4125:创建工作区，指定存在的bucket和objectSharding参数为day
 * @author ZhangYanan
 * @createDate 2022.2.18
 * @updateUser ZhangYanan
 * @updateDate 2022.2.18
 * @updateRemark
 * @version v1.0
 */
public class BucketDirectory4125 extends TestScmBase {
    private boolean runSuccess = false;
    private String wsName = "ws4125";
    private String bucketName = "bucketname4125";
    private ScmShardingType objectIDShardingType = ScmShardingType.DAY;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private AmazonS3 S3Client = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private String fileName = "file_4125";

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
        session = TestScmTools.createSession( site );

        S3Client = CephS3Utils.createConnect( site );
        S3Client.createBucket( bucketName );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        ScmCephS3DataLocation cephS3DataLocation = new ScmCephS3DataLocation(
                site.getSiteName(), bucketName, objectIDShardingType );
        CephS3Utils.createWS( session, wsName, cephS3DataLocation );
        CephS3Utils.wsSetPriority( session, wsName, ScmPrivilegeType.ALL );

        checkBucketNameAndObjectId( wsName, bucketName, objectIDShardingType );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                CephS3Utils.deleteBucket( S3Client, bucketName );
                ScmFactory.Workspace.deleteWorkspace( session, wsName );
                TestTools.LocalFile.removeFile( localPath );
            } finally {
                if ( session != null ) {
                    session.close();
                }
                if ( S3Client != null ) {
                    S3Client.shutdown();
                }
            }
        }
    }

    public void checkBucketNameAndObjectId( String wsName, String expBucketName,
            ScmShardingType objectIDShardingType ) throws ScmException {
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( wsName, session );
        ScmId fileId = ScmFileUtils.create( ws, fileName, filePath );

        String expObjectID = CephS3Utils.getObjectID( wsName,
                objectIDShardingType, fileId );

        boolean doesObjectAndBucketExist = S3Client
                .doesObjectExist( expBucketName, expObjectID );

        Assert.assertTrue( doesObjectAndBucketExist );

        ScmFactory.File.deleteInstance( ws, fileId, true );
    }
}