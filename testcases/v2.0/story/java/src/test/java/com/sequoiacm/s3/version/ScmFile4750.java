package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.module.ScmBucketAttachFailure;
import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4750 :: 开启版本控制，关联多版本文件到桶下
 * @author Zhaoyujing
 * @Date 2020/7/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4750 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4750";
    private String keyName = "aa/bb/object4750";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;
    private String updatePath = null;
    private int updateSize = 1024 * 20;

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

        site = ScmInfo.getSite();
        session = TestScmTools.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.enableVersionControl();
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        // scm create files ,file name
        List<ScmId> keyList = new ArrayList<>();
        ScmId fileId = createScmFile( keyName, filePath );
        keyList.add( fileId );
        updateScmFile( fileId, updatePath );

        // attach files
        List<ScmBucketAttachFailure> failures = ScmFactory.Bucket.attachFile(
                session, bucketName, keyList,
                ScmBucketAttachKeyType.FILE_NAME );
        Assert.assertEquals( failures.size(), 0 );

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
        List<ScmFileBasicInfo> fileList = S3Utils.getVersionList(session, ws, bucketName);
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );

        Assert.assertEquals( fileList.size(), 2 );
        Assert.assertEquals( fileList.get( 0 ).getFileName(), keyName );
        Assert.assertEquals( fileList.get( 0 ).getMajorVersion(), 2 );
        Assert.assertEquals( fileList.get( 1 ).getFileName(), keyName );
        Assert.assertEquals( fileList.get( 1 ).getMajorVersion(), 1 );
        S3Utils.checkFileContent(bucket.getFile(keyName, 2, 0), updatePath, localPath);
        S3Utils.checkFileContent(bucket.getFile(keyName, 1, 0), filePath, localPath);
    }
}
