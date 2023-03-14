package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Descreption SCM-4462:指定FILE_ID为唯一标识，关联文件解除关联后重复关联
 * @Author YiPan
 * @CreateDate 2022/5/18
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4462 extends TestScmBase {
    private String bucketName = "bucket4462";
    private String fileName = "file4462";
    private ScmSession session;
    private AmazonS3 s3Client;
    private ScmWorkspace ws;
    private int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
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
        session = ScmSessionUtils.createSession( rootSite );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
        S3Utils.updateBucketVersionConfig( s3Client, bucketName,
                BucketVersioningConfiguration.ENABLED );
    }

    @Test
    public void test() throws Exception {
        // 创建文件
        List< ScmId > fileIds = createFile();
        ScmId fileID = fileIds.get( 0 );

        // FILE_ID批量关联到S3桶,关联后校验重命名和旧名
        ScmFactory.Bucket.attachFile( session, bucketName, fileIds,
                ScmBucketAttachKeyType.FILE_ID );
        ScmFile file = ScmFactory.File.getInstance( ws, fileID );
        Assert.assertEquals( file.getFileName(), fileID.get() );
        Assert.assertEquals( file.getNameBeforeAttach(), fileName );
        Assert.assertTrue(
                s3Client.doesObjectExist( bucketName, fileID.get() ) );

        // 解除关联
        ScmFactory.Bucket.detachFile( session, bucketName,
                fileIds.get( 0 ).get() );

        // FILE_ID再次关联到S3桶,关联后校验重命名和旧名都为fileId
        ScmFactory.Bucket.attachFile( session, bucketName, fileIds,
                ScmBucketAttachKeyType.FILE_ID );
        file = ScmFactory.File.getInstance( ws, fileID );
        Assert.assertEquals( file.getFileName(), fileID.get() );
        Assert.assertEquals( file.getNameBeforeAttach(), fileID.get() );
        Assert.assertTrue(
                s3Client.doesObjectExist( bucketName, fileID.get() ) );
        runSuccess = true;
    }

    private List< ScmId > createFile() throws ScmException {
        List< ScmId > fileIds = new ArrayList<>();
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( fileName );
        file.setContent( filePath );
        fileIds.add( file.save() );
        return fileIds;
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