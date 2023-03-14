package com.sequoiacm.s3.version.concurrent;

import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.testcommon.ScmInfo;
import com.sequoiacm.testcommon.TestScmBase;
import com.sequoiacm.testcommon.ScmSessionUtils;
import com.sequoiacm.testcommon.listener.GroupTags;
import com.sequoiacm.testcommon.scmutils.S3Utils;
import com.sequoiacm.testcommon.scmutils.ScmFileUtils;
import com.sequoiadb.threadexecutor.ResultStore;
import com.sequoiadb.threadexecutor.ThreadExecutor;
import com.sequoiadb.threadexecutor.annotation.ExecuteOrder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * @descreption SCM-4841 :: 开启版本控制，并发更新相同文件
 * @author Zhaoyujing
 * @Date 2020/7/21
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ScmFile4841 extends TestScmBase {
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4841";
    private String keyName = "aa/bb/object4841";
    private int fileSize = 1024;
    private byte[] fileDatas = new byte[ fileSize ];
    private int versionNum = 10;
    private HashMap< String, byte[] > fileSizeAndContent = new HashMap<>();

    @BeforeClass
    private void setUp() throws Exception {
        session = ScmSessionUtils.createSession( ScmInfo.getSite() );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );

        S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
        ScmBucket bucket = ScmFactory.Bucket.createBucket( ws, bucketName );
        bucket.enableVersionControl();

        ScmFileUtils.createFile( bucket, keyName, fileDatas, keyName );
        fileSizeAndContent.put( fileSize + "", fileDatas );
    }

    @Test(groups = { GroupTags.base })
    public void testCreateBucket() throws Exception {
        ThreadExecutor te = new ThreadExecutor();
        for ( int i = 0; i < versionNum; i++ ) {
            int filesize = 100 + i;
            byte[] updateDatas = new byte[ filesize ];
            new Random().nextBytes( updateDatas );
            fileSizeAndContent.put( filesize + "", updateDatas );
            te.addWorker( new UpdateFileThread( keyName, updateDatas ) );
        }
        te.run();

        checkFileList();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() throws ScmException {
        try {
            if ( runSuccess ) {
                S3Utils.clearBucket( session, s3WorkSpaces, bucketName );
            }
        } finally {
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void checkFileList() throws Exception {
        int count = versionNum + 1;
        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        List< ScmFileBasicInfo > fileList = S3Utils.getVersionList( session, ws,
                bucketName );
        Assert.assertEquals( fileList.size(), versionNum + 1 );

        for ( ScmFileBasicInfo file : fileList ) {
            Assert.assertEquals( file.getMajorVersion(), count );
            Assert.assertEquals( file.getFileName(), keyName );

            ScmFile scmFile = bucket.getFile( file.getFileName(),
                    file.getMajorVersion(), file.getMinorVersion() );
            long size = scmFile.getSize();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            scmFile.getContent( outputStream );
            byte[] downloadData = outputStream.toByteArray();
            Assert.assertEquals( downloadData,
                    fileSizeAndContent.get( size + "" ),
                    "---file size=" + size );
            count--;
        }
    }

    private class UpdateFileThread extends ResultStore {
        String key;
        byte[] content;

        UpdateFileThread( String key, byte[] content ) {
            this.key = key;
            this.content = content;
        }

        @ExecuteOrder(step = 1)
        public void run() throws Exception {
            ScmSession session = null;
            try {
                session = ScmSessionUtils.createSession( ScmInfo.getSite() );
                ScmBucket bucket = ScmFactory.Bucket.getBucket( session,
                        bucketName );
                ScmFile file = bucket.createFile( key );
                file.setContent( new ByteArrayInputStream( content ) );
                file.setAuthor( "author4841" );
                file.save();
            } finally {
                if ( session != null ) {
                    session.close();
                }
            }
        }
    }
}
