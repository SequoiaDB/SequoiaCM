package com.sequoiacm.s3.bucketDirectory.serial;

import java.io.File;

import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.exception.ScmException;

/**
 * @description SCM-4117:创建工作区，指定存在的bucketName
 * @author ZhangYanan
 * @createDate 2022.2.18
 * @updateUser ZhangYanan
 * @updateDate 2022.2.18
 * @updateRemark
 * @version v1.0
 */
public class BucketDirectory4117 extends TestScmBase {
    private boolean runSuccess = false;
    private String wsName = "ws4117";
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String bucketName = "bucket4117";
    private AmazonS3 S3Client = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private String fileName = "file_4117";
    private ScmId fileId = null;

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
        S3Client.createBucket( bucketName );
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        // 指定存在bucketName创建工作区,赋予权限
        ScmCephS3DataLocation cephS3DataLocation = new ScmCephS3DataLocation(
                site.getSiteName(), bucketName, ScmShardingType.NONE );
        ws = CephS3Utils.createWS( session, wsName, cephS3DataLocation );
        CephS3Utils.wsSetPriority( session, wsName, ScmPrivilegeType.ALL );
        fileId = ScmFileUtils.create( ws, fileName, filePath );

        boolean doesObjectAndBucketExist = S3Client
                .doesObjectExist( bucketName, fileId.toString() );
        Assert.assertTrue( doesObjectAndBucketExist );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                S3Client.deleteObject( bucketName, fileId.toString() );
                S3Client.deleteBucket( bucketName );
                ScmFactory.File.deleteInstance( ws, fileId, true );
                ScmFactory.Workspace.deleteWorkspace( session, ws.getName() );
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
}