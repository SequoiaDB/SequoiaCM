package com.sequoiacm.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmCephS3DataLocation;
import com.sequoiacm.client.element.bizconf.ScmDataLocation;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.client.exception.ScmInvalidArgumentException;
import com.sequoiacm.common.ScmShardingType;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.dsutils.CephS3Utils;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiacm.testcommon.scmutils.ScmWorkspaceUtil;

/**
 * @descreption SCM-5596:修改工作区下cephS3数据源配置验证
 * @author ZhangYanan
 * @date 2022/12/22
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class WorkSpaces5596 extends TestScmBase {
    private ScmSession session = null;
    private SiteWrapper site = null;
    private String wsName = "ws5596";
    private String bucketName = "bucket5596";
    private String prefixBucketName = "prefixbucket5596";
    private String fileName = "file5596";
    private ScmWorkspace ws = null;
    private List< SiteWrapper > siteList = new ArrayList<>();
    private int fileSize = 1024 * 1024;
    private AmazonS3 S3Client = null;
    private String filePath = null;
    private File localPath = null;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile2_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSiteByType( ScmType.DatasourceType.CEPH_S3 );
        siteList.add( site );
        session = TestScmTools.createSession( site );

        ScmWorkspaceUtil.deleteWs( wsName, session );

        ScmWorkspaceUtil.createWS( session, wsName, ScmInfo.getSiteNum() );
        ScmWorkspaceUtil.wsSetPriority( session, wsName );
        ws = ScmFactory.Workspace.getWorkspace( wsName, session );

        S3Client = CephS3Utils.createConnect( site );
        S3Client.createBucket( bucketName );
    }

    @Test(groups = { "twoSite", "fourSite", GroupTags.base })
    public void test() throws Exception {
        List< ScmDataLocation > dataLocation = prepareExpWsDataLocation();
        ws.updateDataLocation( dataLocation, true );
        ScmWorkspaceUtil.checkWsUpdate( session, wsName, dataLocation );
        // 通过文件上传下载删除操作验证工作区是否正常
        checkWsStatus();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws Exception {
        try {
        } finally {
            ScmWorkspaceUtil.deleteWs( wsName, session );
            if ( session != null ) {
                session.close();
            }
        }
    }

    public List< ScmDataLocation > prepareExpWsDataLocation()
            throws ScmInvalidArgumentException {
        List< ScmDataLocation > scmDataLocationList = new ArrayList<>();
        ScmCephS3DataLocation scmCephS3DataLocation = new ScmCephS3DataLocation(
                site.getSiteName() );
        scmCephS3DataLocation.setBucketName( bucketName );
        scmCephS3DataLocation.setObjectShardingType( ScmShardingType.NONE );
        scmCephS3DataLocation.setPrefixBucketName( prefixBucketName );
        scmDataLocationList.add( scmCephS3DataLocation );
        return scmDataLocationList;
    }

    public void checkWsStatus() throws Exception {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( filePath );
        file.setFileName( fileName );
        ScmId fileId = file.save();

        SiteWrapper[] expSite = { site };
        ScmFileUtils.checkMetaAndData( wsName, fileId, expSite, localPath,
                filePath );

        // 校验创建的桶内文件是否存在
        boolean doesObjectAndBucketExist = S3Client
                .doesObjectExist( bucketName, fileId.toString() );
        Assert.assertTrue( doesObjectAndBucketExist );

        file.delete( true );
        try {
            ScmFactory.File.getInstance( ws, fileId );
            Assert.fail( "预期失败，实际成功！" );
        } catch ( ScmException e ) {
            if ( e.getErrorCode() != ScmError.FILE_NOT_FOUND.getErrorCode() ) {
                throw e;
            }
        }

    }
}