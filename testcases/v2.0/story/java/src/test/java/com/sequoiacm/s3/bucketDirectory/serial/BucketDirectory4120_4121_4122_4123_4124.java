package com.sequoiacm.s3.bucketDirectory.serial;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmCephS3DataLocation;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;

/**
 * @description SCM-4120:创建工作区，指定objectSharding参数为year
 *              SCM-4121:创建工作区，指定objectSharding参数为quarter
 *              SCM-4122:创建工作区，指定objectSharding参数为month
 *              SCM-4123:创建工作区，指定objectSharding参数为day
 *              SCM-4124:创建工作区，指定objectSharding参数为none
 * @author ZhangYanan
 * @createDate 2022.2.18
 * @updateUser ZhangYanan
 * @updateDate 2022.2.18
 * @updateRemark
 * @version v1.0
 */
public class BucketDirectory4120_4121_4122_4123_4124 extends TestScmBase {
    private String wsNameBase = "ws4120_";
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private String prefixBucketName = "prefixbucketname4120";
    private String expBucketName = null;
    private String expObjectID = null;
    private AmazonS3 S3Client = null;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private String fileName = "file_4120";
    private ScmId fileId = null;
    private AtomicInteger runSuccessCount = new AtomicInteger( 0 );

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
    }

    @Test(groups = { GroupTags.twoSite,
            GroupTags.fourSite }, dataProvider = "dataProvider")
    public void test( ScmShardingType objectIDShardingType, String wsName )
            throws Exception {
        String actObjectID = null;
        // 指定存在bucketName创建工作区,赋予权限,设置objectShardingType
        ScmCephS3DataLocation cephS3DataLocation = new ScmCephS3DataLocation(
                site.getSiteName(), ScmShardingType.NONE, prefixBucketName,
                objectIDShardingType );
        ws = CephS3Utils.createWS( session, wsName, cephS3DataLocation );
        CephS3Utils.wsSetPriority( session, wsName, ScmPrivilegeType.ALL );
        fileId = ScmFileUtils.create( ws, fileName, filePath );

        expBucketName = CephS3Utils.getBucketName( site, wsName );
        expObjectID = CephS3Utils.getObjectID( wsName, objectIDShardingType,
                fileId );

        boolean doesObjectAndBucketExist = S3Client
                .doesObjectExist( expBucketName, expObjectID );
        Assert.assertTrue( doesObjectAndBucketExist );

        // 清理环境
        CephS3Utils.deleteBucket( site, expBucketName );
        ScmFactory.File.deleteInstance( ws, fileId, true );
        ScmFactory.Workspace.deleteWorkspace( session, ws.getName() );

        runSuccessCount.incrementAndGet();
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccessCount.get() == generateDate().length
                || TestScmBase.forceClear ) {
            try {
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

    @DataProvider(name = "dataProvider")
    public Object[][] generateDate() {
        return new Object[][] { { ScmShardingType.YEAR, wsNameBase + "year" },
                { ScmShardingType.QUARTER, wsNameBase + "quarter" },
                { ScmShardingType.MONTH, wsNameBase + "month" },
                { ScmShardingType.DAY, wsNameBase + "day" },
                { ScmShardingType.NONE, wsNameBase + "none" } };
    }
}