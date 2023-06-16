package com.sequoiacm.s3.object;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.Tag;
import com.sequoiacm.client.core.ScmFactory;
import com.sequoiacm.client.core.ScmFile;
import com.sequoiacm.client.core.ScmSession;
import com.sequoiacm.client.core.ScmWorkspace;
import com.sequoiacm.client.element.ScmId;
import com.sequoiacm.client.exception.ScmException;
import com.sequoiacm.exception.ScmError;
import com.sequoiacm.testcommon.*;
import com.sequoiacm.testcommon.scmutils.S3Utils;

/**
 * @descreption SCM-5519:设置对象标签超出上限
 * @author YiPan
 * @date 2022/12/8
 * @updateUser
 * @updateDate
 * @updateRemark
 * @version 1.0
 */
public class ObjectTag5519 extends TestScmBase {
    private AmazonS3 s3Client = null;
    private SiteWrapper site = null;
    private ScmSession session = null;
    private List< Tag > tagSet = new ArrayList<>();
    private Map< String, String > map = new HashMap<>();
    private String bucketName;
    private String keyName = "Object5519";
    private ScmWorkspace ws;
    private File localPath = null;
    private String filePath = null;
    private int fileSize = 1024;
    private boolean runSuccess = false;

    @BeforeClass
    private void setUp() throws Exception {
        localPath = new File( TestScmBase.dataDirectory + File.separator
                + TestTools.getClassName() );
        filePath = localPath + File.separator + "localFile_" + fileSize
                + ".txt";
        TestTools.LocalFile.removeFile( localPath );
        TestTools.LocalFile.createDir( localPath.toString() );
        TestTools.LocalFile.createFile( filePath, fileSize );
        bucketName = TestScmBase.bucketName;

        s3Client = S3Utils.buildS3Client();
        site = ScmInfo.getSite();
        session = ScmSessionUtils.createSession( site );
        ws = ScmFactory.Workspace.getWorkspace( TestScmBase.s3WorkSpaces,
                session );
        initTag();
        s3Client.putObject( bucketName, keyName, filePath );
    }

    // SEQUOIACM-1418
    @Test(enabled = false)
    public void test() throws ScmException {
        testS3API();

        testSCMAPI();
        runSuccess = true;
    }

    @AfterClass
    private void tearDown() {
        try {
            if ( runSuccess || TestScmBase.forceClear ) {
                s3Client.deleteObject( bucketName, keyName );
                TestTools.LocalFile.removeFile( localPath );
            }
        } finally {
            if ( s3Client != null ) {
                s3Client.shutdown();
            }
            if ( session != null ) {
                session.close();
            }
        }
    }

    private void initTag() {
        String baseKey = "test";
        String baseValue = "value";
        for ( int i = 0; i < 9; i++ ) {
            tagSet.add( new Tag( baseKey + i, baseValue + i ) );
            map.put( baseKey + i, baseValue + i );
        }
    }

    private void testSCMAPI() throws ScmException {
        String baseKey = "test";
        String baseValue = "value";
        ScmId fileID = S3Utils.queryS3Object( ws, keyName );
        ScmFile file = ScmFactory.File.getInstance( ws, fileID );

        // 设置标签<10
        file.setCustomTag( map );
        Assert.assertEquals( file.getCustomTag(), map );

        // 设置标签=10
        map.put( baseKey + 9, baseValue + 9 );
        file.setCustomTag( map );
        Assert.assertEquals( file.getCustomTag(), map );

        // 设置标签>10
        try {
            map.put( baseKey + 10, baseValue + 10 );
            file.setCustomTag( map );
            Assert.fail( "except fail but success" );
        } catch ( ScmException e ) {
            Assert.assertEquals( e.getError(),
                    ScmError.FILE_CUSTOMTAG_TOO_LARGE );
        }
    }

    private void testS3API() {
        String baseKey = "test";
        String baseValue = "value";
        // 设置标签<10
        S3Utils.setObjectTag( s3Client, bucketName, keyName, tagSet );
        GetObjectTaggingResult tagging = s3Client.getObjectTagging(
                new GetObjectTaggingRequest( bucketName, keyName ) );
        S3Utils.compareTagSet( tagging.getTagSet(), tagSet );

        // 设置标签=10
        tagSet.add( new Tag( baseKey + 9, baseValue + 9 ) );
        S3Utils.setObjectTag( s3Client, bucketName, keyName, tagSet );
        tagging = s3Client.getObjectTagging(
                new GetObjectTaggingRequest( bucketName, keyName ) );
        S3Utils.compareTagSet( tagging.getTagSet(), tagSet );

        // 设置标签>10
        try {
            tagSet.add( new Tag( baseKey + 10, baseValue + 10 ) );
            S3Utils.setObjectTag( s3Client, bucketName, keyName, tagSet );
            Assert.fail( "except fail but success" );
        } catch ( AmazonS3Exception e ) {
            if ( !e.getErrorCode().contains( "BadRequest" ) ) {
                throw e;
            }
        }

    }
}
