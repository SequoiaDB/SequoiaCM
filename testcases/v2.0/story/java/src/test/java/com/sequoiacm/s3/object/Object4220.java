package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Descreption SCM-4220:SCM文件关联桶后，S3接口删除文件
 * @Author YiPan
 * @CreateDate 2022/5/13
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4220 extends TestScmBase {
    private final String bucketName = "bucket4220";
    private final String objectKey = "object4220";
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

    @Test
    public void test() throws Exception {
        // 创建桶
        ScmFactory.Bucket.createBucket( ws, bucketName );

        // 创建SCM文件挂载
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setFileName( objectKey );
        file.setContent( filePath );
        ScmId fileID = file.save();
        ScmFactory.Bucket.attachFile( session, bucketName, fileID );

        // S3接口删除
        s3Client.deleteObject( bucketName, objectKey );

        //校验删除结果
        Assert.assertFalse( s3Client.doesObjectExist( bucketName, objectKey ) );
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        long fileNum = bucket.countFile( null );
        Assert.assertEquals( fileNum, 0 );

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