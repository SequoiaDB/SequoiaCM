package com.sequoiacm.s3.object;

import com.amazonaws.services.s3.AmazonS3;
import com.sequoiacm.client.core.*;
import com.sequoiacm.client.element.ScmFileBasicInfo;
import com.sequoiacm.client.exception.ScmException;
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
 * @author Zhaoyujing
 * @version 1.0
 * @descreption SCM-4223 :: S3接口创建S3文件，SCM API列取文件
 * @Date 2020/5/10
 * @updateUser
 * @updateDate
 * @updateRemark
 */
public class Object4223 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private WsWrapper wsp = null;
    private ScmSession session = null;
    private ScmWorkspace ws = null;
    private boolean runSuccess = false;
    private String bucketName = "bucket4223";
    private String key = "aa/bb/object4223";
    private File localPath = null;
    private String updatePath = null;
    private String filePath = null;
    private int fileSize = 1024 * 10;
    private int updateSize = 1024 * 20;
    private int objectNums = 30;

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
        s3Client = S3Utils.buildS3Client();
        S3Utils.clearBucket( s3Client, bucketName );

        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( s3WorkSpaces, session );
    }

    @Test
    public void test() throws ScmException {
        // s3 create bucket
        s3Client.createBucket( bucketName );
        // s3 put objects
        List< String > keyList = s3PutObjects();
        // scm list files in bucket, check file list
        List< String > getKeyList = scmGetFiles();

        Assert.assertEqualsNoOrder( keyList.toArray(), getKeyList.toArray() );

        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        if ( runSuccess ) {
            S3Utils.clearBucket( s3Client, bucketName );
            TestTools.LocalFile.removeFile( localPath );
        }

        if ( s3Client != null ) {
            s3Client.shutdown();
        }

        if ( session != null ) {
            session.close();
        }
    }

    private List< String > s3PutObjects() {
        List< String > keyList = new ArrayList<>();
        for ( int i = 0; i < objectNums; i++ ) {
            String keyName = key + "_" + i;
            keyList.add( keyName );
            s3Client.putObject( bucketName, keyName, new File( filePath ) );
        }
        return keyList;
    }

    private List< String > scmGetFiles() throws ScmException {
        List< String > getKeyList = new ArrayList<>();

        ScmBucket bucket = ScmFactory.Bucket.getBucket( session, bucketName );
        ScmCursor< ScmFileBasicInfo > filesCursor = bucket.listFile( null, null,
                0, -1 );
        while ( filesCursor.hasNext() ) {
            ScmFileBasicInfo file = filesCursor.getNext();
            getKeyList.add( file.getFileName() );
        }

        return getKeyList;
    }
}
