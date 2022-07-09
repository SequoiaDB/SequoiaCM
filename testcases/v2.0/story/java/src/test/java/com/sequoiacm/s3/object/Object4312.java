package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.*;
import com.sequoiacm.client.element.privilege.ScmPrivilegeType;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
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
    private final String wsName = "ws4312";
    private SiteWrapper rootSite;
    private SiteWrapper branchSite;
    private ScmSession session;
    private AmazonS3 s3Client = null;
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

        // 新建ws,ws仅包含主站点
        createWs( wsName );
        ScmAuthUtils.alterUser( session, wsName,
                ScmFactory.User.getUser( session, TestScmBase.scmUserName ),
                ScmFactory.Role.getRole( session, "ROLE_AUTH_ADMIN" ),
                ScmPrivilegeType.ALL );
        ScmAuthUtils.checkPriorityByS3( session, wsName );

        // 使用分站点s3节点url建立连接
        s3Client = S3Utils.buildS3Client();
    }

    @Test(groups = "twoSite")
    public void test() throws Exception {
        // 在主站点ws下创建桶
        s3Client.createBucket( bucketName );

        // 创建文件，校验文件存在
        s3Client.putObject( bucketName, objectKey, new File( filePath ) );
        Assert.assertTrue( s3Client.doesObjectExist( bucketName, objectKey ) );

        // 更新文件
        s3Client.putObject( bucketName, objectKey, new File( updatePath ) );

        // 下载文件，校验更新后的内容
        S3Object object = s3Client.getObject( bucketName, objectKey );
        S3Utils.inputStream2File( object.getObjectContent(), downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( updatePath ) );

        // 删除文件，校验文件不存在
        s3Client.deleteObject( bucketName, objectKey );
        Assert.assertFalse( s3Client.doesObjectExist( bucketName, objectKey ) );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                ScmFactory.Workspace.deleteWorkspace( session, wsName, true );
            }
        } finally {
            s3Client.shutdown();
            session.close();
        }
    }

    private void createWs( String wsName ) throws Exception {
        ScmWorkspaceUtil.deleteWs( wsName, session );
        ScmWorkspaceConf conf = new ScmWorkspaceConf();
        String domain = TestSdbTools
                .getDomainNames( ScmInfo.getRootSite().getDataDsUrl() )
                .get( 0 );
        ScmSdbDataLocation sdbDataLocation = new ScmSdbDataLocation(
                rootSite.getSiteName(), domain );
        List< ScmDataLocation > dataLocationList = new ArrayList<>();
        dataLocationList.add( sdbDataLocation );
        conf.setDataLocations( dataLocationList );
        conf.setMetaLocation(
                ScmWorkspaceUtil.getMetaLocation( ScmShardingType.YEAR ) );
        conf.setEnableDirectory( false );
        conf.setName( wsName );
        ScmWorkspaceUtil.createWS( session, conf );
    }
}