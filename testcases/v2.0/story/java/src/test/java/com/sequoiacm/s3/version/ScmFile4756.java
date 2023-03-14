package com.sequoiacm.s3.version;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.element.bizconf.ScmUploadConf;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.common.module.ScmBucketAttachKeyType;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @descreption SCM-4756 :: 关联版本文件指定不存在的桶
 * @author Zhaoyujing
 * @Date 2020/7/23
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4756 extends TestScmBase {
    private SiteWrapper site = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4756";
    private String keyName = "aa/bb/object4756";
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ScmFileUtils.cleanFile( ws, keyName );
    }

    @Test
    public void test() throws ScmException, IOException {
        // scm create files ,file name
        List< ScmId > keyList = new ArrayList<>();
        ScmId fileId = createScmFile( keyName, filePath );
        keyList.add( fileId );

        // attach files
        try {
            ScmFactory.Bucket.attachFile( session, bucketName, keyList,
                    ScmBucketAttachKeyType.FILE_NAME );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError().getErrorType(),
                    "BUCKET_NOT_EXISTS" );
        }

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( session, bucketName );
                ScmFileUtils.cleanFile( ws, keyName );
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
}
