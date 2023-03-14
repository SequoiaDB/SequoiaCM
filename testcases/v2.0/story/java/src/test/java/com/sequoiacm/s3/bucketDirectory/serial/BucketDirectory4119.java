package com.sequoiacm.s3.bucketDirectory.serial;

import java.io.File;

import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
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
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;

/**
 * @description SCM-4119:创建工作区，不指定bucketName
 * @author ZhangYanan
 * @createDate 2022.2.18
 * @updateUser ZhangYanan
 * @updateDate 2022.2.18
 * @updateRemark
 * @version v1.0
 */
public class BucketDirectory4119 extends TestScmBase {
    private boolean runSuccess = false;
    private String wsName = "ws4119";
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String prefixBucketName = "prefixbucketname4119";
    private String expBucketName = null;
    private AmazonS3 S3Client = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private String fileName = "file_4119";
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
    }

    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite })
    public void test() throws Exception {
        String actBucketName = null;
        // 不指定创建工作区,赋予权限,指定bucketShardingType和prefixBucketName
        ScmCephS3DataLocation cephS3DataLocation = new ScmCephS3DataLocation(
                site.getSiteName(), ScmShardingType.YEAR, prefixBucketName );
        ws = CephS3Utils.createWS( session, wsName, cephS3DataLocation );
        CephS3Utils.wsSetPriority( session, wsName, ScmPrivilegeType.ALL );
        fileId = ScmFileUtils.create( ws, fileName, filePath );

        expBucketName = CephS3Utils.getBucketName( site, wsName );

        boolean doesObjectAndBucketExist = S3Client
                .doesObjectExist( expBucketName, fileId.toString() );
        Assert.assertTrue( doesObjectAndBucketExist );
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        if ( runSuccess || TestScmBase.forceClear ) {
            try {
                CephS3Utils.deleteBucket( site,expBucketName );
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