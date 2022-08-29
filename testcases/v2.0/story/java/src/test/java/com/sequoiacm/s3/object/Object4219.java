package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.S3Object;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @Descreption SCM-4219:SCM文件关联桶后，S3接口查询文件
 * @Author YiPan
 * @CreateDate 2022/5/13
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4219 extends TestScmBase {
    private final String bucketName = "bucket4219";
    private final String objectKey = "object4219";
    private ScmSession session;
    private AmazonS3 s3Client;
    private ScmWorkspace ws;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private String downloadPath = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        SiteWrapper rootSite = ScmInfo.getRootSite();
        session = TestScmTools.createSession( rootSite );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // 创建桶
        s3Client.createBucket( bucketName );
        S3Utils.updateBucketVersionConfig( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );

        // 创建自由标签元数据
        Map< String, String > map = new HashMap<>();
        map.put( "test", "sequoiadb" );
        map.put( "num", "100" );

        // 创建scm文件，指定自由标签
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( objectKey );
        file.setContent( filePath );
        file.setCustomMetadata( map );
        ScmId fileID = file.save();

        // 关联文件
        ScmFactory.Bucket.attachFile( session, bucketName, fileID );

        // 查询校验元数据
        S3Object object = s3Client.getObject( bucketName, objectKey );
        Assert.assertEquals( object.getObjectMetadata().getUserMetadata(),
                map );
        Assert.assertEquals( object.getBucketName(), bucketName );

        // 查询校验数据
        downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        S3Utils.inputStream2File( object.getObjectContent(), downloadPath );
        Assert.assertEquals( TestTools.getMD5( downloadPath ),
                TestTools.getMD5( filePath ) );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( s3Client, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            session.close();
            s3Client.shutdown();
        }
    }

}