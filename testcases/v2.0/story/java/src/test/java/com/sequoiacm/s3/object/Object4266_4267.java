package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
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

/**
 * @Descreption SCM-4266:指定FILE_NAME为唯一标识，批量关联文件 SCM-4267:指定FILE_ID为唯一标识，批量关联文件
 * @Author YiPan
 * @CreateDate 2022/5/18
 * @UpdateUser
 * @UpdateDate
 * @UpdateRemark
 * @Version
 */
public class Object4266_4267 extends TestScmBase {
    private String bucketName = "bucket4266";
    private String fileNameBase = "file4266";
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
        session = TestScmTools.createSession( rootSite );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );
        s3Client.createBucket( bucketName );
    }

    @Test
    public void test() throws Exception {
        // 清理、创建文件
        List< ScmId > fileIds = cleanAndCreateFile( null );

        // FILE_ID批量关联到S3桶,关联后校验重命名
        ScmFactory.Bucket.attachFile( session, bucketName, fileIds,
                ScmBucketAttachKeyType.FILE_ID );
        for ( int i = 0; i < fileIds.size(); i++ ) {
            checkAttachFileByFileId( fileIds.get( i ), fileNameBase + i );
        }

        // 清理、创建文件
        fileIds = cleanAndCreateFile( fileIds );

        // FILE_NAME批量关联到S3桶,关联后校验
        ScmFactory.Bucket.attachFile( session, bucketName, fileIds,
                ScmBucketAttachKeyType.FILE_NAME );
        for ( int i = 0; i < fileIds.size(); i++ ) {
            checkAttachFileByFileName( fileIds.get( i ), fileNameBase + i );
        }
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

    private List< ScmId > cleanAndCreateFile( List< ScmId > fileIds )
            throws ScmException {
        // fileIds = null时无需清理
        if ( fileIds != null ) {
            for ( ScmId fileID : fileIds ) {
                ScmFactory.File.deleteInstance( ws, fileID, true );
            }

        }
        fileIds = new ArrayList<>();
        for ( int i = 0; i < 10; i++ ) {
            ScmFile file = ScmFactory.File.createInstance( ws );
            file.setFileName( fileNameBase + i );
            file.setContent( filePath );
            // TODO:SEQUOIACM-845
            fileIds.add( file.save( new ScmUploadConf( true, true ) ) );

        }
        return fileIds;
    }

    private void checkAttachFileByFileId( ScmId fileId,
            String beforeAttachName ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getFileName(), fileId.get() );
        Assert.assertEquals( file.getNameBeforeAttach(), beforeAttachName );
        Assert.assertTrue(
                s3Client.doesObjectExist( bucketName, fileId.get() ) );
    }

    private void checkAttachFileByFileName( ScmId fileId,
            String beforeAttachName ) throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        Assert.assertEquals( file.getFileName(), beforeAttachName );
        Assert.assertEquals( file.getNameBeforeAttach(), null );
        Assert.assertTrue(
                s3Client.doesObjectExist( bucketName, beforeAttachName ) );
    }
}