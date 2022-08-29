package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @descreption SCM-4736 :: 不开启版本控制，增加同名文件
 * @author Zhaoyujing
 * @Date 2020/7/13
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4736 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4736";
    private String key = "object4736";
    private File localPath = null;
    private String filePath = null;
    private String updatePath = null;
    private int fileSize = 1024 * 10;
    private int updateSize = 1024 * 20;
    private String user_meta_key = "test";
    private String user_meta_value1 = "aaaa";
    private String user_meta_value2 = "bbbb";

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
        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        ScmFactory.Bucket.createBucket( ws, bucketName );
    }

    @Test(groups = { GroupTags.base })
    public void test() throws Exception {
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        createScmFile( bucket, filePath, user_meta_value1 );
        createScmFile( bucket, updatePath, user_meta_value2 );

        checkFile( bucket, key, updatePath );

        List< ScmFileBasicInfo > getKeyList = S3Utils.getVersionList(session, ws, bucketName);
        Assert.assertEquals( getKeyList.size(), 1 );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void createScmFile( ScmBucket bucket, String path, String meta )
            throws ScmException {
        ScmFile file = bucket.createFile( key );
        file.setContent( path );
        file.setAuthor( "author4736" );
        Map< String, String > customMeta = new HashMap<>();
        customMeta.put( user_meta_key, meta );
        file.setCustomMetadata( customMeta );

        file.save();
    }

    private void checkFile( ScmBucket bucket, String objectName, String path )
            throws Exception {
        ScmFile file = bucket.getFile( objectName );
        file.getMd5();
        String downloadPath = TestTools.LocalFile.initDownloadPath( localPath,
                TestTools.getMethodName(), Thread.currentThread().getId() );
        OutputStream fileOutputStream = new FileOutputStream( downloadPath );
        file.getContent( fileOutputStream );
        fileOutputStream.close();

        String updateMD5 = TestTools.getMD5( path );
        String downloadMD5 = TestTools.getMD5( downloadPath );
        Assert.assertEquals( updateMD5, downloadMD5 );

        Assert.assertEquals( file.getCustomMetadata().get( user_meta_key ),
                user_meta_value2 );
        Assert.assertTrue(file.isNullVersion());
    }
}
