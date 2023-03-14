package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.bson.BSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @Descreption SCM-4255:SCM API创建S3文件 SCM-4256:SCM API列取桶内文件
 * @Author YiPan
 * @CreateDate 2022/5/16
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4255_4256 extends TestScmBase {
    private final String bucketName = "bucket4255";
    private final String objectKey = "object4255no";
    private List< String > objectKeys = new ArrayList<>();
    private ScmSession session;
    private AmazonS3 s3Client = null;
    private ScmWorkspace ws;
    private final int fileSize = 1024 * 300;
    private final int fileNum = 30;
    private File localPath = null;
    private String filePathA = null;
    private String filePathB = null;
    private boolean runSuccess = false;

    @BeforeClass
    public void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePathA = localPath + File.separator + "localFileA_" + fileSize
                + ".txt";
        filePathB = localPath + File.separator + "localFileB_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePathA, fileSize );
        TestTools.LocalFile.createFile( filePathB, fileSize / 2 );

        session = ScmSessionUtils.createSession( ScmInfo.getRootSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // 创建桶，创建多个文件
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        creatFiles( bucket );

        // 列取所有文件，查询下载文件内容
        ScmCursor< ScmFileBasicInfo > scmFileBasicInfoScmCursor = bucket
                .listFile( null, null, 0, -1 );
        checkContentByObjectKeys( scmFileBasicInfoScmCursor, objectKeys,
                false );

        // 使用匹配条件列取
        BSONObject cond = ScmQueryBuilder.start( ScmAttributeName.File.SIZE )
                .is( fileSize ).get();
        BSONObject orderBy = ScmQueryBuilder
                .start( ScmAttributeName.File.FILE_NAME ).is( -1 ).get();
        scmFileBasicInfoScmCursor = bucket.listFile( cond, orderBy, 5, 3 );
        List< String > expObjectKeys = new ArrayList<>();
        expObjectKeys.add( objectKey + 27 );
        expObjectKeys.add( objectKey + 25 );
        expObjectKeys.add( objectKey + 23 );
        checkContentByObjectKeys( scmFileBasicInfoScmCursor, expObjectKeys,
                true );
        runSuccess = true;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if ( runSuccess ) {
                TestTools.LocalFile.removeFile( localPath );
                S3Utils.clearBucket( s3Client, bucketName );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
            session.close();
        }
    }

    private void creatFiles( ScmBucket bucket ) throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = bucket.createFile( objectKey + i );
            if ( i % 2 == 1 ) {
                file.setContent( filePathA );
            } else {
                file.setContent( filePathB );
            }
            file.save();
            objectKeys.add( objectKey + i );
        }
    }

    private void checkContentByObjectKeys(
            ScmCursor< ScmFileBasicInfo > scmFileBasicInfoScmCursor,
            List< String > expFileName, boolean sort )
            throws IOException, ScmException {
        List< String > actFileName = new ArrayList<>();
        String md5A = TestTools.getMD5( filePathA );
        String md5B = TestTools.getMD5( filePathB );
        // 下载校验数据
        while ( scmFileBasicInfoScmCursor.hasNext() ) {
            ScmFileBasicInfo fileInfo = scmFileBasicInfoScmCursor.getNext();
            actFileName.add( fileInfo.getFileName() );
            ScmFile file = ScmFactory.File.getInstance( ws,
                    fileInfo.getFileId() );
            String downloadPath = localPath + File.separator
                    + fileInfo.getFileName() + UUID.randomUUID() + ".txt";
            file.getContent( downloadPath );
            if ( file.getSize() == fileSize ) {
                Assert.assertEquals( TestTools.getMD5( downloadPath ), md5A );
            } else {
                Assert.assertEquals( TestTools.getMD5( downloadPath ), md5B );
            }
        }
        // 是否排序校验结果
        if ( sort ) {
            Assert.assertEquals( expFileName, actFileName );
        } else {
            Assert.assertEqualsNoOrder( expFileName.toArray(),
                    actFileName.toArray() );
        }
    }
}