package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.common.ScmType;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @Descreption SCM-4225:S3接口创建S3文件，SCM API删除文件
 * @Author YiPan
 * @CreateDate 2022/5/12
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4225 extends TestScmBase {
    private final String bucketName = "bucket4225";
    private final String objectKey = "object4225";
    private ScmSession session;
    private AmazonS3 s3Client;
    private ScmWorkspace ws;
    private final int fileSize = 1024 * 300;
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
        session = TestScmTools.createSession( rootSite );

        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() throws Exception {
        // s3接口创建文件
        s3Client.putObject( bucketName, objectKey, new File( filePath ) );

        // scm api删除文件
        ScmId fileId = S3Utils.queryS3Object( ws, objectKey );
        ScmFactory.File.deleteInstance( ws, fileId, true );

        // 校验文件已删除
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