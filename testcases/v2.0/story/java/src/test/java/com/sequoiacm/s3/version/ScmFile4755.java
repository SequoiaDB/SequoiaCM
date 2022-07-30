package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.module.ScmBucketAttachFailure;
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
 * @descreption SCM-4755 :: 开启版本控制，关联批次下多个版本文件到桶下
 * @author Zhaoyujing
 * @Date 2020/7/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4755 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4755";
    private String keyName = "aa/bb/object4755";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;
    private String updatePath = null;
    private int updateSize = 1024 * 20;
    private int objectNum = 10;
    private int versionNum = 2;
    private List< String > filePathList = new ArrayList<>();

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        updatePath = localPath + File.separator + "localFile_" + updateSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        TestTools.LocalFile.createFile( updatePath, updateSize );
        filePathList.add( filePath );
        filePathList.add( updatePath );

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.enableVersionControl();
    }

    @Test
    public void test() throws Exception {
        // scm create files ,file name
        List< ScmId > keyList1 = new ArrayList<>();
        for ( int i = 0; i < objectNum / 2; i++ ) {
            String key = keyName + "-" + i;
            ScmId fileId = createScmFile( key, filePath );
            keyList1.add( fileId );
            updateScmFile( fileId, updatePath );
        }

        // attach files
        List< ScmBucketAttachFailure > failures1 = ScmFactory.Bucket.attachFile(
                session, bucketName, keyList1,
                ScmBucketAttachKeyType.FILE_NAME );
        Assert.assertEquals( failures1.size(), 0 );

        List< ScmId > keyList2 = new ArrayList<>();
        for ( int i = objectNum / 2; i < objectNum; i++ ) {
            String key = keyName + "-" + i;
            ScmId fileId = createScmFile( key, filePath );
            keyList2.add( fileId );
            updateScmFile( fileId, updatePath );
        }

        // attach files
        List< ScmBucketAttachFailure > failures2 = ScmFactory.Bucket.attachFile(
                session, bucketName, keyList2,
                ScmBucketAttachKeyType.FILE_NAME );
        Assert.assertEquals( failures2.size(), 0 );

        checkFileList();

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( session, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private ScmId createScmFile( String keyName, String path )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws );
        file.setContent( path );
        file.setFileName( keyName );
        file.save( new ScmUploadConf( true, true ) );
        return file.getFileId();
    }

    private void updateScmFile( ScmId fileId, String path )
            throws ScmException {
        ScmFile file = ScmFactory.File.getInstance( ws, fileId );
        file.updateContent( path );
    }

    private void checkFileList() throws Exception {
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        List< ScmFileBasicInfo > fileList = S3Utils.getVersionList( session, ws,
                bucketName );

        Assert.assertEquals( fileList.size(), objectNum * versionNum );
        int count = 0;
        for ( ScmFileBasicInfo file : fileList ) {
            int expectVersion = versionNum - count % versionNum;
            String key = keyName + "-" + count / versionNum;
            Assert.assertEquals( file.getFileName(),
                    key );
            Assert.assertEquals( file.getMajorVersion(), expectVersion );
            S3Utils.checkFileContent(
                    bucket.getFile( key, expectVersion, 0 ),
                    filePathList.get( expectVersion - 1 ), localPath );

            count++;
        }
    }
}
