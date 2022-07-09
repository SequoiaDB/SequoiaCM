package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
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
import java.util.UUID;

/**
 * @Descreption SCM-4218:SCM文件关联桶后，S3接口列取文件
 * @Author YiPan
 * @CreateDate 2022/5/12
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4218 extends TestScmBase {
    private String bucketName = "bucket4218";
    private List< ScmId > fileIds = new ArrayList<>();
    private ScmSession session;
    private AmazonS3 s3Client;
    private ScmWorkspace ws;
    private int fileSize = 1024 * 300;
    private int fileNum = 10;
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
        // 创建多个文件
        createFile();

        // 批量挂载到S3桶
        ScmFactory.Bucket.attachFile( session, bucketName, fileIds,
                ScmBucketAttachKeyType.FILE_ID );

        // s3接口列取校验元数据
        ObjectListing objectListing = s3Client.listObjects( bucketName );
        List< ScmId > actObjectKeys = new ArrayList<>();
        List< S3ObjectSummary > objectSummaries = objectListing
                .getObjectSummaries();
        for ( int i = 0; i < objectSummaries.size(); i++ ) {
            String key = objectSummaries.get( i ).getKey();
            actObjectKeys.add( new ScmId( key ) );
        }
        Assert.assertEqualsNoOrder( fileIds.toArray(),
                actObjectKeys.toArray() );

        // 校验数据
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        for ( int i = 0; i < fileIds.size(); i++ ) {
            S3Object object = s3Client.getObject( bucketName,
                    fileIds.get( i ).toString() );
            TestTools.LocalFile.removeFile( downloadPath );
            S3Utils.inputStream2File( object.getObjectContent(), downloadPath );
            Assert.assertEquals( TestTools.getMD5( downloadPath ),
                    TestTools.getMD5( filePath ) );
        }
        runSuccess = true;
    }

    private void createFile() throws ScmException {
        for ( int i = 0; i < fileNum; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( UUID.randomUUID().toString() );
            file.setContent( filePath );
            // TODO:SEQUOIACM-845
            fileIds.add( file.save( new ScmUploadConf( true, true ) ) );
        }
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