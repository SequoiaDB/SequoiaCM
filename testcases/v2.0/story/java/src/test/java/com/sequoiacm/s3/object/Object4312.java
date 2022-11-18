package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmAuthUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-4312:使用S3协议跨站点上传下载文件
 * @Author YiPan
 * @CreateDate 2022/5/18
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4312 extends TestScmBase {
    private final String bucketName = "bucket4312";
    private final String objectKey = "object4312";
    private SiteWrapper rootSite;
    private SiteWrapper branchSite;
    private ScmSession session;
    private ScmSession branchSiteSession;
    private AmazonS3 rootS3Client = null;
    private AmazonS3 branchS3Client = null;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private String downloadPath = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "updateFile_" + fileSize
                + ".txt";
        downloadPath = localPath + File.separator + "downLoadFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, fileSize );
        session = TestScmTools.createSession( ScmInfo.getRootSite() );

        rootSite = ScmInfo.getRootSite();
        branchSite = ScmInfo.getBranchSite();

        // 直连不同节点
        branchS3Client = S3Utils.buildS3Client( TestScmBase.s3AccessKeyID,
                TestScmBase.s3SecretKey, getS3NodeURLBySite( session,
                        branchSite.getSiteServiceName() ) );
        rootS3Client = S3Utils.buildS3Client( TestScmBase.s3AccessKeyID,
                TestScmBase.s3SecretKey,
                getS3NodeURLBySite( session, rootSite.getSiteServiceName() ) );
        branchSiteSession = TestScmTools
                .createSession( branchSite.getSiteName() );
        S3Utils.clearBucket( rootS3Client, bucketName );
    }

    // SEQUOIACM-1147
    @Test(groups = { GroupTags.twoSite, GroupTags.fourSite,
            GroupTags.base }, enabled = false)
    public void test() throws Exception {
        // 在主站点ws下创建桶，创建文件
        rootS3Client.createBucket( bucketName );

        // 主站点创建文件，校验文件存在
        rootS3Client.putObject( bucketName, objectKey, new File( filePath ) );
        Assert.assertTrue(
                branchS3Client.doesObjectExist( bucketName, objectKey ) );

        // 分站点更新文件
        branchS3Client.putObject( bucketName, objectKey,
                new File( updatePath ) );

        // 分站点下载文件，校验更新后的内容
        S3Object object = branchS3Client.getObject( bucketName, objectKey );
        S3Utils.inputStream2File( object.getObjectContent(), downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( updatePath ) );

        // detach File
        ScmFactory.Bucket.detachFile( branchSiteSession, bucketName,
                objectKey );

        // attach File
        ScmWorkspace ws = ScmFactory.Workspace
                .getWorkspace( TestScmBase.s3WorkSpaces, branchSiteSession );
        ScmId scmId = S3Utils.queryS3Object( ws, objectKey );
        ScmFactory.Bucket.attachFile( branchSiteSession, bucketName, scmId );

        // 删除文件，校验文件不存在
        branchS3Client.deleteObject( bucketName, objectKey );
        Assert.assertFalse(
                branchS3Client.doesObjectExist( bucketName, objectKey ) );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( branchS3Client, bucketName );
            }
        } finally {
            rootS3Client.shutdown();
            branchS3Client.shutdown();
            session.close();
            branchSiteSession.close();
        }
    }

    private static String getS3NodeURLBySite( ScmSession session,
            String siteName ) throws ScmException {
        ScmCursor< ScmHealth > scmHealthScmCursor = ScmSystem.Monitor
                .listHealth( session, null );
        String S3URL = null;
        while ( scmHealthScmCursor.hasNext() ) {
            ScmHealth scmHealth = scmHealthScmCursor.getNext();
            String serviceName = scmHealth.getServiceName();
            if ( serviceName.contains( "s3" ) ) {
                if ( serviceName.contains( siteName ) ) {
                    S3URL = scmHealth.getNodeName();
                    break;
                }
            }
        }
        scmHealthScmCursor.close();
        return "http://" + S3URL;
    }
}