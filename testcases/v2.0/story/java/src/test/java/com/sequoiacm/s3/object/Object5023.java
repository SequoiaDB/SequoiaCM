package com.sequoiacm.s3.object;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.s3.model.*;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.TestScmTools;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.TestTools;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiadb.exception.BaseException;

/**
 * @descreption SCM-5023:SCM API创建桶内文件，接口参数校验
 * @author YiPan
 * @date 2022/7/27
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class Object5023 extends TestScmBase {
    private String bucketName = "bucket5023";
    private ScmSession session;
    private ScmBucket bucket;
    private int fileSize = 1024 * 1024;
    private File localPath = null;
    private String filePath = null;
    private AtomicInteger actSuccessTests = new AtomicInteger( 0 );

    @DataProvider(name = "legalKeyNameProvider")
    public Object[][] generateKeyName() {
        return new Object[][] {
                // test a : 范围内取值
                new Object[] { "dir1/test.txt" },
                // test b : 长度边界值
                new Object[] { TestTools.getRandomString( 1 ) },
                new Object[] { TestTools.getRandomString( 900 ) },
                // test c : 包含特殊字符
                new Object[] { "!-_.'()" },
                // test d : 包含 数字字符[0-9a-zA-Z]
                new Object[] {
                        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" },
                // test e : 包含需要特殊处理的字符
                new Object[] { "&@,$=+" },
                // test f : 包含不建议使用的字符
                new Object[] { "^`{}][#~" },
                // test g : 包含中文字符
                new Object[] { "测试对象名" },
                // test objectKey length>900
                new Object[] { TestTools.getRandomString( 901 ) } };
    }

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        session = TestScmTools.createSession();
        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ScmWorkspace ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces,
                session );
        bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
    }

    @Test(dataProvider = "legalKeyNameProvider")
    public void testLegalKeyName( String keyName ) throws Exception {
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        String downLoadPath = localPath + File.separator + "downLoadFile_"
                + fileSize + ".txt";
        ScmFile file = bucket.createFile( keyName );
        file.setContent( filePath );
        file.save();
        file.getContent( downLoadPath );
        Assert.assertEquals( TestTools.getMD5( downLoadPath ),
                TestTools.getMD5( filePath ),
                "md5 is wrong! the key name is : " + keyName );
        TestTools.LocalFile.removeFile( localPath );
        actSuccessTests.getAndIncrement();
    }

    @Test
    public void testIllegalKeyName() throws Exception {
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        // test a : 对象名为空串
        try {
            bucket.createFile( "" );
            Assert.fail( "when key name is '',it should fail" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT );
        }

        try {
            bucket.createFile( null );
            Assert.fail( "when key name is null,it should fail" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(), ScmError.INVALID_ARGUMENT );
        }
        actSuccessTests.getAndIncrement();
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( actSuccessTests.get() == ( generateKeyName().length + 1 ) ) {
                S3Utils.clearBucket( session, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            session.close();
        }
    }
}
