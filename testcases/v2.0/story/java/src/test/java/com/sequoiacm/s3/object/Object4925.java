package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestScmTools;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @descreption SCM-4925:使用S3接口更新对象，验证对象关联的文件fileId无变化
 * @author YiPan
 * @date 2022/7/9
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object4925 extends TestScmBase {
    private AmazonS3 s3Client;
    private final String bucketName = "bucket4925";
    private final String objectKey = "object4925";
    private ScmSession session;
    private ScmWorkspace ws;
    private final int fileSize = 1024 * 300;
    private File localPath = null;
    private String filePath = null;
    private String updateFilePath = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updateFilePath = localPath + File.separator + "updateFile_" + fileSize
                + ".txt";

        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updateFilePath, fileSize );
        session = TestScmTools.createSession( ScmInfo.getRootSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test
    public void test() throws Exception {
        // 创建桶，创建s3文件
        s3Client.createBucket( bucketName );
        s3Client.putObject( bucketName, objectKey, new File( filePath ) );

        // 获取s3文件的fileId
        ScmId expfileId = S3Utils.queryS3Object( ws, objectKey );

        // 上传文件更新内容
        s3Client.putObject( bucketName, objectKey, new File( updateFilePath ) );

        // 校验更新后的fileId
        ScmId actfileId = S3Utils.queryS3Object( ws, objectKey );
        Assert.assertEquals( actfileId, expfileId );
    }

    @AfterClass
    public void tearDown() {
        try {
            if ( runSuccess ) {
                TestTools.LocalFile.removeFile( localPath );
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            session.close();
            s3Client.shutdown();
        }
    }
}
